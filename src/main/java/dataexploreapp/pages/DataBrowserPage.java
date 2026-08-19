package dataexploreapp.pages;
import dataexploreapp.aggregation.AggregationFunction;
import dataexploreapp.controllers.DataBrowserController;
import dataexploreapp.db_config.database.ForeignKeyInfo;
import dataexploreapp.db_config.dataquality.DataQualityAnalyzer;
import dataexploreapp.db_config.dataquality.DataQualityReport;
import dataexploreapp.dialogs.ExportDialog;
import dataexploreapp.dialogs.LoginDialog;
import dataexploreapp.dialogs.ProceduresParamsDialog;
import dataexploreapp.dialogs.RunQueryDialog;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.database.ProcedureParameter;
import dataexploreapp.auth.AuthService;
import dataexploreapp.filtering.FilterCombinator;
import dataexploreapp.filtering.FilterCondition;
import dataexploreapp.filtering.FilterOperator;
import java.io.File;
import java.io.IOException;

import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import dataexploreapp.charts.ChartBuilder;
import dataexploreapp.history.ExportRecord;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DataBrowserPage {
    @FunctionalInterface
    private interface PaginationCall {
        DataTable run() throws Exception;
    }
    private VBox createDataQualitySummary(int rowCount, double qualityScore, int duplicateRows, double missingPercentage, int invalidEmails) {
        Label scoreLabel = new Label(String.format("Quality Score  %.0f%%", qualityScore));
        ProgressBar progressBar = new ProgressBar(qualityScore / 100.0);
        if (qualityScore >= 90) {
            progressBar.getStyleClass().add("quality-good");
        } else if (qualityScore >= 70) {
            progressBar.getStyleClass().add("quality-warning");
        } else {
            progressBar.getStyleClass().add("quality-bad");
        }

        progressBar.setMaxWidth(Double.MAX_VALUE);
        Label issuesLabel = new Label(String.format("%d duplicate rows  •  %.1f%% missing  •  %d invalid email addresses", duplicateRows, missingPercentage, invalidEmails));

        VBox box = new VBox(8, scoreLabel, progressBar, issuesLabel);
        box.setPadding(new Insets(12));
        return box;
    }
    /** One row in the filter builder: column, operator, value(s), remove button. */
    private class FilterRowControls {
        final ComboBox<String> columnBox = new ComboBox<>();
        final ComboBox<FilterOperator> operatorBox = new ComboBox<>(FXCollections.observableArrayList(FilterOperator.values()));
        final TextField valueField = new TextField();
        final TextField secondValueField = new TextField();
        final HBox container;

        FilterRowControls() {
            columnBox.setItems(currentColumns);
            if (!currentColumns.isEmpty()) columnBox.getSelectionModel().selectFirst();
            operatorBox.getSelectionModel().select(FilterOperator.CONTAINS);
            valueField.setPromptText("value");
            secondValueField.setPromptText("and value");
            secondValueField.setVisible(false);
            secondValueField.setManaged(false);

            operatorBox.valueProperty().addListener((obs, old, val) -> {
                boolean needsSecond = val != null && val.requiresSecondValue();
                secondValueField.setVisible(needsSecond);
                secondValueField.setManaged(needsSecond);
            });

            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> removeFilterRow(this));
            removeBtn.setStyle("-fx-text-fill:red;");
            removeBtn.setMinWidth(25);
            removeBtn.setPrefWidth(25);
            removeBtn.setMaxWidth(25);
            removeBtn.setPadding(new Insets(0));

            columnBox.setPrefWidth(90);
            operatorBox.setPrefWidth(55);
            valueField.setPrefWidth(45);
            secondValueField.setPrefWidth(45);

            container = new HBox(
                    4,
                    columnBox,
                    operatorBox,
                    valueField,
                    secondValueField,
                    removeBtn
            );

            container.setAlignment(Pos.CENTER_LEFT);
        }

        FilterCondition toCondition() {
            FilterOperator operator = operatorBox.getValue();
            String secondValue = operator != null && operator.requiresSecondValue() ? secondValueField.getText() : null;
            return new FilterCondition(columnBox.getValue(), operator, valueField.getText(), secondValue);
        }

        boolean isComplete() {
            FilterCondition c = toCondition();
            if (c.getColumn() == null || c.getOperator() == null || c.getValue() == null || c.getValue().isBlank()) return false;
            return !c.getOperator().requiresSecondValue() || (c.getSecondValue() != null && !c.getSecondValue().isBlank());
        }
    }
    private final String username;
    private final DataBrowserController controller;

    private final ListView<String> tableList = new ListView<>();
    private final TableView<ObservableList<Object>> resultsTable = new TableView<>();
    private final ListView<String> procedureList = new ListView<>();

    private final ObservableList<String> currentColumns = FXCollections.observableArrayList();
    private final ComboBox<FilterCombinator> filterCombinatorBox = new ComboBox<>(FXCollections.observableArrayList(FilterCombinator.values()));
    private final VBox filterRowsBox = new VBox(6);
    private final List<FilterRowControls> filterRows = new java.util.ArrayList<>();
    private final ComboBox<String> sortColumnBox = new ComboBox<>();
    private final ComboBox<String> sortDirectionBox =
            new ComboBox<>(FXCollections.observableArrayList("Ascending", "Descending"));

    private final ComboBox<ChartBuilder.ChartType> chartTypeBox =
            new ComboBox<>(FXCollections.observableArrayList(ChartBuilder.ChartType.values()));
    private final ComboBox<String> chartCategoryBox = new ComboBox<>();
    private final VBox chartSeriesBox = new VBox(5);
    private final Map<String, CheckBox> chartSeriesCheckBoxes = new LinkedHashMap<>();
    private final VBox columnsBox = new VBox(5);
    private final Map<String, CheckBox> columnCheckBoxes = new LinkedHashMap<>();
    private final Label statusLabel = new Label();
    private final BorderPane chartArea = new BorderPane();
    private final VBox foreignKeysBox = new VBox(6);
    private final VBox dataQualityBox = new VBox(8);
    private final ComboBox<String> aggGroupColumnBox = new ComboBox<>();
    private final ComboBox<String> aggValueColumnBox = new ComboBox<>();
    private final ComboBox<AggregationFunction> aggFunctionBox =
            new ComboBox<>(FXCollections.observableArrayList(AggregationFunction.values()));

    private final ComboBox<String> chartAggFunctionBox =
            new ComboBox<>(FXCollections.observableArrayList("None", "SUM", "AVG", "COUNT", "MIN", "MAX"));
    private Stage stage;

    private final Label paginationLabel = new Label();
    private final Button prevPageBtn = new Button("◀ Prev");
    private final Button nextPageBtn = new Button("Next ▶");
    private final ComboBox<Integer> pageSizeBox =
            new ComboBox<>(FXCollections.observableArrayList(50, 100, 500, 1000));
    private final Button loadFullTableBtn = new Button("Load entire table");


    public DataBrowserPage(String username, DataBaseReader reader, String schema) {
        this.username = username;
        this.controller = new DataBrowserController(reader, schema);
    }

    public void show(Stage stage) {
        this.stage = stage;

        // TABLES PANEL
        Label tablesLabel = new Label("Tables");
        tablesLabel.setMinHeight(25);
        tablesLabel.setStyle("-fx-text-fill: black;");

        VBox tablesPanel = new VBox(8, tablesLabel, tableList);
        tablesPanel.setPadding(new Insets(10));
        tablesPanel.setPrefWidth(200);
        tablesPanel.setPrefHeight(250);

        VBox.setVgrow(tableList, Priority.ALWAYS);

        tableList.setOnMouseClicked(e -> {
            String selected = tableList.getSelectionModel().getSelectedItem();

            if (selected != null) {
                loadTableAsync(selected, null);
            }
        });


        // PROCEDURES PANEL
        Label proceduresLabel = new Label("Procedures");
        proceduresLabel.setMinHeight(25);
        proceduresLabel.setStyle("-fx-text-fill: black;");

        VBox proceduresPanel = new VBox(8, proceduresLabel, procedureList);
        proceduresPanel.setPadding(new Insets(10));
        proceduresPanel.setPrefWidth(200);
        proceduresPanel.setPrefHeight(180);

        VBox.setVgrow(procedureList, Priority.ALWAYS);

        procedureList.setOnMouseClicked(e -> {
            String selected = procedureList.getSelectionModel().getSelectedItem();

            if (selected != null) {
                promptAndLoadProcedureAsync(selected, null);
            }
        });


        // COLUMNS PANEL
        Label columnsLabel = new Label("Columns");
        columnsLabel.setMinHeight(25);
        columnsLabel.setStyle("-fx-text-fill: black;");

        columnsBox.setPadding(new Insets(10));
        columnsBox.setPrefWidth(220);

        ScrollPane columnsScroll = new ScrollPane(columnsBox);
        columnsScroll.setFitToWidth(true);
        columnsScroll.setPrefWidth(220);

        VBox.setVgrow(columnsScroll, Priority.ALWAYS);


        // RELATED TABLES PANEL
        ScrollPane fkScroll = new ScrollPane(foreignKeysBox);
        fkScroll.setFitToWidth(true);
        fkScroll.setPrefWidth(220);
        fkScroll.setMaxHeight(200);



        foreignKeysBox.setPadding(new Insets(8));
        foreignKeysBox.setPrefWidth(220);

        Label relatedTablesLabel = new Label("Related tables");
        relatedTablesLabel.setStyle("-fx-text-fill: black;");
        relatedTablesLabel.setMinHeight(25);

        VBox foreignKeysPanel = new VBox(
                8,
                relatedTablesLabel,
                fkScroll
        );

        foreignKeysPanel.setPadding(new Insets(10));
        foreignKeysPanel.setPrefWidth(240);
        foreignKeysPanel.setPrefHeight(220);


        // COLUMN BUTTONS
        Button selectAllColumns = new Button("Select All");

        selectAllColumns.setOnAction(e -> {
            for (CheckBox checkBox : columnCheckBoxes.values()) {
                checkBox.setSelected(true);
            }

            updateVisibleColumns();
        });


        Button clearAllColumns = new Button("Clear All");

        clearAllColumns.setOnAction(e -> {
            for (CheckBox checkBox : columnCheckBoxes.values()) {
                checkBox.setSelected(false);
            }

            updateVisibleColumns();
        });


        HBox columnButtons = new HBox(
                8,
                selectAllColumns,
                clearAllColumns
        );


        VBox columnsPanel = new VBox(
                8,
                columnsLabel,
                columnButtons,
                columnsScroll
        );

        columnsPanel.setPadding(new Insets(10));
        columnsPanel.setPrefWidth(240);
        columnsPanel.setPrefHeight(280);

        // LEFT SIDEBAR
        VBox leftPanel = new VBox(
                10,
                tablesPanel,
                proceduresPanel,
                columnsPanel,
                foreignKeysPanel
        );

        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(240);
        leftPanel.setMinWidth(240);

// Make the entire left sidebar scrollable
        ScrollPane leftScrollPane = new ScrollPane(leftPanel);
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScrollPane.setPrefWidth(260);
        leftScrollPane.setMinWidth(260);
        leftScrollPane.setMaxWidth(260);

        // CENTER PANEL
        dataQualityBox.setVisible(false);
        dataQualityBox.setManaged(false);
        HBox paginationBar = buildPaginationBar();

        VBox centerPanel = new VBox(10, dataQualityBox,resultsTable, paginationBar, statusLabel,chartArea);

        resultsTable.setMinHeight(400);
        chartArea.setMinHeight(600);
        chartArea.setPrefHeight(600);
        chartArea.setMaxWidth(Double.MAX_VALUE);


        centerPanel.setPadding(new Insets(10));
        ScrollPane centerScrollPane = new ScrollPane(centerPanel);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScrollPane.setMinWidth(260);
        centerScrollPane.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane();

        root.setTop(buildNavbar());
        root.setLeft(leftScrollPane);
        root.setRight(buildControlsScrollPane());
        root.setCenter(centerScrollPane);

        BorderPane.setMargin(centerPanel, new Insets(10));


        // SCENE
        Scene scene = new Scene(root, 1530, 800);

        var cssUrl = getClass().getResource("databrowser.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }



        // SHOW WINDOW
        stage.setTitle("Data Browser");
        stage.setScene(scene);

        // Start maximized like the layout you provided
        stage.setMaximized(true);

        stage.show();


        // LOAD DATA
        loadTableListAsync();
        loadProceduresListAsync();
    }

    private HBox buildNavbar() {
        HBox navbar = new HBox(12);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2b2b2b;");

        Label navLabel = new Label("Welcome, " + username + "!");
        navLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String navBtnStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;";

        Button historyBtn = new Button("History");
        historyBtn.setStyle(navBtnStyle);
        historyBtn.setOnAction(e -> new ExportHistoryPage(this).show(new Stage()));

        Button exportBtn = new Button("Export data");
        exportBtn.setStyle(navBtnStyle);
        exportBtn.setOnAction(e -> openExportDialog());

        Button settingsBtn = new Button("Account");
        settingsBtn.setStyle(navBtnStyle);
        settingsBtn.setOnAction(e -> {
            try {
                new AccountPage(username, controller.getReader(), controller.getSchema()).show(stage);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        Button importBtn = new Button("Import file");
        importBtn.setStyle(navBtnStyle);
        importBtn.setOnAction(e -> chooseAndImportFile());

        Button queryBtn = new Button("Run query");
        queryBtn.setStyle(navBtnStyle);
        queryBtn.setOnAction(e -> {
            Optional<String> query = RunQueryDialog.show(stage);
            query.ifPresent(this::runQueryAsync);
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(navBtnStyle);
        logoutBtn.setOnAction(e -> {
            new LoginDialog(new AuthService()).show(new Stage());
            stage.close();
        });

        navbar.getChildren().addAll(settingsBtn, spacer, historyBtn, importBtn, queryBtn, exportBtn, logoutBtn);        return navbar;
    }

    private VBox buildControlsPanel() {
        Button addFilterBtn = new Button("+ Add condition");
        addFilterBtn.setOnAction(e -> addFilterRow());
        Button applyFiltersBtn = new Button("Apply filters");
        applyFiltersBtn.setOnAction(e -> applyFilters());
        Button clearFiltersBtn = new Button("Clear");
        clearFiltersBtn.setOnAction(e -> clearFilters());

        filterCombinatorBox.getSelectionModel().select(FilterCombinator.AND);
        addFilterRow(); // start with one empty row



        aggFunctionBox.getSelectionModel().select(AggregationFunction.COUNT);
        Button applyAggBtn = new Button("Apply aggregation");
        applyAggBtn.setOnAction(e -> applyAggregation());

        // COUNT doesn't need a value column — disable it when COUNT is selected
        aggFunctionBox.valueProperty().addListener((obs, old, val) ->
                aggValueColumnBox.setDisable(val != null && !val.requiresValueColumn()));
        aggValueColumnBox.setDisable(false);
        chartAggFunctionBox.getSelectionModel().selectFirst();
        chartTypeBox.getSelectionModel().selectFirst();
        Button showChartBtn = new Button("Show chart");
        showChartBtn.getStyleClass().add("primary-button");
        showChartBtn.setOnAction(e -> renderChart());

        Button saveChartBtn = new Button("Save chart as image");
        saveChartBtn.setOnAction(e -> saveChartAsImage());
        Button applySortBtn = new Button("Apply sort");
        applySortBtn.setOnAction(e -> applySort());

        Label filter=new Label("Filter:");
        filter.setStyle("-fx-text-fill: black;");
        Label sort=new Label("Sort:");
        sort.setStyle("-fx-text-fill: black;");
        Label aggregate=new Label("Aggregate:");
        aggregate.setStyle("-fx-text-fill: black;");
        Label chart=new Label("Chart:");
        chart.setStyle("-fx-text-fill: black;");
        Label va=new Label("Value / Aggregation:");
        va.setStyle("-fx-text-fill: black;");
        VBox filterSection = new VBox(6,
                filter,
                new HBox(8, new Label("Combine with:"), filterCombinatorBox),
                filterRowsBox,
                addFilterBtn,
                new HBox(8, applyFiltersBtn, clearFiltersBtn)
        );
        VBox panel = new VBox(10,
                filterSection,
                new Separator(),
                sort, sortColumnBox, sortDirectionBox, applySortBtn,
                new Separator(),
                aggregate, aggGroupColumnBox, aggValueColumnBox, aggFunctionBox, applyAggBtn,
                new Separator(),
                chart, chartTypeBox, chartCategoryBox,
                va, chartAggFunctionBox,
                new Label("Series:"),
                chartSeriesBox,
                showChartBtn, saveChartBtn
        );
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(240);
        panel.setMinWidth(240);
        panel.setMaxWidth(Double.MAX_VALUE);

        return panel;
    }

    // --- Async wrappers around the controller ---------------------------------

    private void loadTableListAsync() {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception { return controller.listTables(); }
        };
        task.setOnSucceeded(e -> tableList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> statusLabel.setText("Failed to list tables: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void loadProceduresListAsync() {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception { return controller.listProcedures(); }
        };
        task.setOnSucceeded(e -> procedureList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> statusLabel.setText("Failed to list procedures: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void loadTableAsync(String tableName, ExportRecord historyRecord) {
        statusLabel.setText("Loading " + tableName + "...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.loadTable(tableName); }
        };
        task.setOnSucceeded(e -> {
            onFreshTableLoaded(historyRecord);
            loadForeignKeysAsync(tableName);
        });
        task.setOnFailed(e -> {
            String message = task.getException().getMessage();
            if (message != null && message.contains("ORA-04044")) {
                loadProcedureAsync(tableName, Map.of(), historyRecord);
            } else {
                statusLabel.setText("Failed to load table: " + message);
            }
        });
        new Thread(task).start();
    }

    private void loadForeignKeysAsync(String tableName) {
        foreignKeysBox.getChildren().clear();
        Task<List<ForeignKeyInfo>> task = new Task<>() {
            @Override protected List<ForeignKeyInfo> call() throws Exception { return controller.loadForeignKeys(tableName); }
        };
        task.setOnSucceeded(e -> {
            List<ForeignKeyInfo> fks = task.getValue();
            if (fks.isEmpty()) {
                foreignKeysBox.getChildren().add(new Label("No foreign keys."));
                return;
            }
            for (ForeignKeyInfo fk : fks) {
                CheckBox checkBox = new CheckBox(fk.describe());
                checkBox.setSelected(false);
                checkBox.setOnAction(ev -> onForeignKeySelectionChanged());
                foreignKeysBox.getChildren().add(checkBox);
            }
        });
        task.setOnFailed(e -> foreignKeysBox.getChildren().setAll(new Label("Failed to load foreign keys.")));
        new Thread(task).start();
    }

    private void onForeignKeySelectionChanged() {
        List<ForeignKeyInfo> allFks = controller.getCurrentForeignKeys();
        List<ForeignKeyInfo> enabled = new java.util.ArrayList<>();
        for (int i = 0; i < foreignKeysBox.getChildren().size(); i++) {
            if (foreignKeysBox.getChildren().get(i) instanceof CheckBox checkBox && checkBox.isSelected()) {
                enabled.add(allFks.get(i));
            }
        }
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.reloadTableWithForeignKeys(enabled); }
        };
        task.setOnSucceeded(e -> {
            populateColumnPickers();
            renderTable(controller.getCurrentTable());
            statusLabel.setText("Loaded " + controller.getCurrentTable().getRowCount() + " rows.");
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to reload with joins: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void promptAndLoadProcedureAsync(String procedureName, ExportRecord historyRecord) {
        statusLabel.setText("Checking parameters for " + procedureName + "...");
        Task<List<ProcedureParameter>> task = new Task<>() {
            @Override protected List<ProcedureParameter> call() throws Exception { return controller.getInputParameters(procedureName); }
        };
        task.setOnSucceeded(e -> {
            List<ProcedureParameter> inputParams = task.getValue();
            if (inputParams.isEmpty()) {
                loadProcedureAsync(procedureName, Map.of(), historyRecord);
            } else {
                Optional<Map<String, String>> values = ProceduresParamsDialog.show(stage, procedureName, inputParams);
                if (values.isPresent()) {
                    loadProcedureAsync(procedureName, values.get(), historyRecord);
                } else {
                    statusLabel.setText("Cancelled.");
                }
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to check parameters: " + task.getException().getMessage()));
        new Thread(task).start();
    }
    private ScrollPane buildControlsScrollPane() {

        VBox controlsPanel = buildControlsPanel();

        ScrollPane scrollPane = new ScrollPane(controlsPanel);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.setPrefWidth(260);
        scrollPane.setMinWidth(260);
        scrollPane.setMaxWidth(260);

        return scrollPane;
    }
    private void loadProcedureAsync(String procedureName, Map<String, String> paramValues, ExportRecord historyRecord) {
        statusLabel.setText("Loading " + procedureName + "...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.loadProcedure(procedureName, paramValues); }
        };
        task.setOnSucceeded(e -> {
            foreignKeysBox.getChildren().setAll(new Label("N/A"));
            onFreshTableLoaded(historyRecord);
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to load procedure: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void chooseAndImportFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import data from file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All supported", "*.csv", "*.xlsx", "*.xls", "*.docx", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("Word", "*.docx"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadFileAsync(file, null);
        }
    }

    private void loadFileAsync(File file, ExportRecord historyRecord) {
        statusLabel.setText("Importing " + file.getName() + "...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.loadFile(file); }
        };
        task.setOnSucceeded(e -> {
            foreignKeysBox.getChildren().setAll(new Label("N/A"));
            if (historyRecord != null) {
                applyReapplyOutcome(controller.reapplyRecord(historyRecord));
            } else {
                populateColumnPickers();
                renderTable(controller.getCurrentTable());
                statusLabel.setText("Imported " + controller.getCurrentTable().getRowCount() + " rows from " + file.getName() + ".");
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Import failed: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Common tail for loadTableAsync/loadProcedureAsync: refresh pickers, render, or reapply history. */
    private void onFreshTableLoaded(ExportRecord historyRecord) {
        populateColumnPickers();

        if (historyRecord != null) {
            applyReapplyOutcome(controller.reapplyRecord(historyRecord));
        } else {

            renderTable(controller.getCurrentTable());
            DataTable table = controller.getCurrentTable();
            List<Map<String, Object>> qualityRows = new ArrayList<>();

            for (Object[] row : table.getRows()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();

                for (int i = 0; i < table.getColumnNames().size(); i++) {
                    rowMap.put(table.getColumnNames().get(i), row[i]);
                }

                qualityRows.add(rowMap);
            }

            DataQualityReport report = DataQualityAnalyzer.analyze(qualityRows);

            dataQualityBox.getChildren().setAll(
                    createDataQualitySummary(
                            table.getRowCount(),
                            report.getScore(),
                            report.getDuplicateRows(),
                            report.getMissingPercentage(),
                            report.getInvalidEmails()
                    ));

            dataQualityBox.setVisible(true);
            dataQualityBox.setManaged(true);
            statusLabel.setText("Loaded " + table.getRowCount() + " rows.");
        }
    }

    private void applyReapplyOutcome(DataBrowserController.ReapplyOutcome outcome) {
        resetFilterRowsUI();
        if (outcome.hasFilter()) {
            FilterRowControls row = filterRows.get(0);
            row.columnBox.setValue(outcome.filterColumn());
            row.operatorBox.setValue(FilterOperator.CONTAINS);
            row.valueField.setText(outcome.filterValue());
        }
        if (outcome.hasSort()) {
            sortColumnBox.setValue(outcome.sortColumn());
            sortDirectionBox.setValue(Boolean.TRUE.equals(outcome.sortAscending()) ? "Ascending" : "Descending");
        }

        renderTable(outcome.resultTable());

        if (!outcome.hasFilter() && !outcome.hasSort()) {
            statusLabel.setText("This export had no filter or sort — loaded full table (" + outcome.resultTable().getRowCount() + " rows).");
        } else {
            statusLabel.setText("Reapplied " + (outcome.hasFilter() ? "filter" : "")
                    + (outcome.hasFilter() && outcome.hasSort() ? " + " : "")
                    + (outcome.hasSort() ? "sort" : "") + " from history.");
        }
    }

    /** Called by ExportHistoryPage when the user clicks "Reapply filters." */
    public void applyHistoryRecord(ExportRecord record) {
        stage.toFront();
        stage.requestFocus();

        if (record.getFilePath() != null) {
            File file = new File(record.getFilePath());
            if (!file.exists()) {
                statusLabel.setText("Original file no longer exists: " + record.getFilePath());
                return;
            }
            loadFileAsync(file, record);
        } else if (record.isProcedure()) {
            procedureList.getSelectionModel().select(record.getTableName());
            loadProcedureAsync(record.getTableName(), Map.of(), record);
        } else {
            tableList.getSelectionModel().select(record.getTableName());
            loadTableAsync(record.getTableName(), record);
        }
    }

    // sort / chart: UI events, delegate to controller synchronously


    private void applySort() {
        String column = sortColumnBox.getValue();
        if (column == null) return;
        try {
            DataTable result = controller.applySort(column, "Ascending".equals(sortDirectionBox.getValue()));
            renderTable(result);
        } catch (IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void renderChart() {
        DataTable table = controller.getCurrentTable();

        if (table == null) {
            statusLabel.setText("Load a table first.");
            return;
        }

        String category = chartCategoryBox.getValue();

        if (category == null) {
            statusLabel.setText("Select a category column.");
            return;
        }

        ChartBuilder.ChartType chartType = chartTypeBox.getValue();

        if (chartType == null) {
            statusLabel.setText("Select a chart type.");
            return;
        }

        String aggMode = chartAggFunctionBox.getValue();

        // NO AGGREGATION

        if (aggMode == null || "None".equals(aggMode)) {

            List<String> selectedSeries =
                    chartSeriesCheckBoxes.entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().isSelected())
                            .map(Map.Entry::getKey)
                            .toList();

            if (selectedSeries.isEmpty()) {
                statusLabel.setText("Select at least one series.");
                return;
            }

            Chart chart = ChartBuilder.build(
                    table,
                    chartType,
                    category,
                    selectedSeries
            );

            chartArea.setCenter(chart);
            return;
        }

        // AGGREGATION

        AggregationFunction function =
                AggregationFunction.valueOf(aggMode);

        List<String> selectedSeries =
                chartSeriesCheckBoxes.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().isSelected())
                        .map(Map.Entry::getKey)
                        .toList();

        // Aggregation (currently supports one value column)
        String value = selectedSeries.isEmpty()
                ? null
                : selectedSeries.get(0);

        if (function.requiresValueColumn() && value == null) {
            statusLabel.setText(
                    "Pick at least one series for " + function + "."
            );
            return;
        }

        DataTable aggregated =
                controller.buildAggregatedChartData(
                        category,
                        value,
                        function
                );

        List<String> aggregatedColumns =
                aggregated.getColumnNames();

        if (aggregatedColumns.size() < 2) {
            statusLabel.setText("Invalid aggregated chart data.");
            return;
        }

        Chart chart = ChartBuilder.build(
                aggregated,
                chartType,
                aggregatedColumns.get(0),
                List.of(aggregatedColumns.get(1))
        );

        chartArea.setCenter(chart);
    }

    private void saveChartAsImage() {
        if (chartArea.getCenter() == null) {
            statusLabel.setText("Show a chart first.");
            return;
        }

        DataTable table = controller.getCurrentTable();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save chart as image");
        chooser.setInitialFileName(table != null ? table.getTitle() + "_chart.png" : "chart.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage snapshot = chartArea.getCenter().snapshot(params, null);

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
            statusLabel.setText("Saved chart: " + file.getName());
        } catch (IOException ex) {
            statusLabel.setText("Failed to save chart: " + ex.getMessage());
        }
    }

    private void openExportDialog() {
        DataTable table = controller.getCurrentTable();
        if (table == null) {
            statusLabel.setText("Load a table first.");
            return;
        }

        List<FilterCondition> activeConditions = filterRows.stream()
                .filter(FilterRowControls::isComplete)
                .map(FilterRowControls::toCondition)
                .toList();

        String filterColumn = null;
        String filterValue = null;
        if (!activeConditions.isEmpty()) {
            filterColumn = activeConditions.size() == 1 ? activeConditions.get(0).getColumn() : "Multiple";
            String joiner = filterCombinatorBox.getValue() == FilterCombinator.OR ? " OR " : " AND ";
            filterValue = activeConditions.stream()
                    .map(FilterCondition::describe)
                    .collect(Collectors.joining(joiner));
        }

        String sortColumn = sortColumnBox.getValue();
        Boolean sortAscending = sortColumn == null ? null : "Ascending".equals(sortDirectionBox.getValue());

        new ExportDialog(table, table.getTitle(), controller.isCurrentProcedure(), controller.getCurrentImportedFilePath(),
                filterColumn, filterValue, sortColumn, sortAscending)
                .show(new Stage());
    }

    // --- Rendering ---------------------------------------------------------

    private void updateVisibleColumns() {
        DataTable table = controller.getCurrentTable();
        if (table == null) return;
        renderTable(table);
    }

    private void populateColumnPickers() {
        DataTable original = controller.getOriginalTable();

        ObservableList<String> columns =
                FXCollections.observableArrayList(original.getColumnNames());

        currentColumns.setAll(columns);

        resetFilterRowsUI();

        sortColumnBox.setItems(columns);
        chartCategoryBox.setItems(columns);

        aggGroupColumnBox.setItems(columns);
        aggValueColumnBox.setItems(columns);

        if (!columns.isEmpty()) {
            aggGroupColumnBox.getSelectionModel().selectFirst();
            aggValueColumnBox.getSelectionModel().selectFirst();

            sortColumnBox.getSelectionModel().selectFirst();
            chartCategoryBox.getSelectionModel().selectFirst();
        }
        columnsBox.getChildren().clear();
        columnCheckBoxes.clear();

        for (String column : columns) {
            CheckBox checkBox = new CheckBox(column);
            // Show all columns by default
            checkBox.setSelected(true);
            columnCheckBoxes.put(column, checkBox);
            columnsBox.getChildren().add(checkBox);
            // Refresh table when checkbox changes
            checkBox.setOnAction(e -> updateVisibleColumns());
        }

        chartSeriesBox.getChildren().clear();
        chartSeriesCheckBoxes.clear();

        for (String column : columns) {
            CheckBox checkBox = new CheckBox(column);
            checkBox.setSelected(false);
            chartSeriesCheckBoxes.put(column, checkBox);
            chartSeriesBox.getChildren().add(checkBox);
        }
    }

    private void renderTable(DataTable dataTable) {
        resultsTable.getColumns().clear();
        List<String> columns = dataTable.getColumnNames();

        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            CheckBox checkBox = columnCheckBoxes.get(columnName);
            if (checkBox != null && !checkBox.isSelected()) continue;

            final int colIndex = i;
            TableColumn<ObservableList<Object>, Object> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().get(colIndex)));
            resultsTable.getColumns().add(column);
        }

        ObservableList<ObservableList<Object>> rows = FXCollections.observableArrayList();
        for (Object[] row : dataTable.getRows()) {
            rows.add(FXCollections.observableArrayList(row));
        }
        resultsTable.setItems(rows);
    }
    private void applyAggregation() {
        String groupColumn = aggGroupColumnBox.getValue();
        String valueColumn = aggValueColumnBox.getValue();
        AggregationFunction function = aggFunctionBox.getValue();
        if (groupColumn == null || function == null) return;
        if (function.requiresValueColumn() && valueColumn == null) {
            statusLabel.setText("Pick a value column for " + function + ".");
            return;
        }

        try {
            DataTable result = controller.applyAggregation(groupColumn, valueColumn, function);
            renderTable(result);
            statusLabel.setText("Aggregated into " + result.getRowCount() + " groups.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
    private void runQueryAsync(String sql) {
        statusLabel.setText("Running query...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.runRawQuery(sql); }
        };
        task.setOnSucceeded(e -> {
            foreignKeysBox.getChildren().setAll(new Label("N/A"));
            populateColumnPickers();
            renderTable(controller.getCurrentTable());
            statusLabel.setText("Loaded " + controller.getCurrentTable().getRowCount() + " rows.");
        });
        task.setOnFailed(e -> statusLabel.setText("Query failed: " + task.getException().getMessage()));
        new Thread(task).start();
    }
    private void goToPreviousPage() {
        runPaginationAction(controller::previousPage);
    }

    private void goToNextPage() {
        runPaginationAction(controller::nextPage);
    }

    private void changePageSize() {
        Integer size = pageSizeBox.getValue();
        if (size == null) return;
        runPaginationAction(() -> controller.setPageSize(size));
    }
    private void updatePaginationControl() {
        boolean paginated = controller.isPaginated();

        prevPageBtn.setDisable(!paginated || controller.getCurrentPageDisplay() <= 1);
        nextPageBtn.setDisable(!paginated || controller.getCurrentPageDisplay() >= controller.getTotalPages());
        pageSizeBox.setDisable(!paginated);
        loadFullTableBtn.setDisable(!paginated);

        if (controller.isFullTableLoaded()) {
            paginationLabel.setText("Full table loaded (" + controller.getCurrentTable().getRowCount() + " rows)");
        } else if (paginated) {
            paginationLabel.setText("Page " + controller.getCurrentPageDisplay() + " of " + controller.getTotalPages()
                    + " (" + controller.getTotalRowCount() + " rows)");
        } else {
            paginationLabel.setText("N/A");
        }
    }
    private void loadFullTableAsync() {
        statusLabel.setText("Loading entire table — this may take a while for large tables...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return controller.loadFullTable(); }
        };
        task.setOnSucceeded(e -> {
            populateColumnPickers();
            renderTable(controller.getCurrentTable());
            updatePaginationControl();
            statusLabel.setText("Loaded full table: " + controller.getCurrentTable().getRowCount() + " rows.");
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to load full table: " + task.getException().getMessage()));
        new Thread(task).start();
    }


    // run the paging call in the background, then repopulate pickers + render + update the bar.
    private void runPaginationAction(PaginationCall action) {
        statusLabel.setText("Loading page...");
        Task<DataTable> task = new Task<>() {
            @Override protected DataTable call() throws Exception { return action.run(); }
        };
        task.setOnSucceeded(e -> {
            populateColumnPickers();
            renderTable(controller.getCurrentTable());
            updatePaginationControl();
            statusLabel.setText("Page " + controller.getCurrentPageDisplay() + " of " + controller.getTotalPages()
                    + " (" + controller.getTotalRowCount() + " total rows).");
        });
        task.setOnFailed(e -> statusLabel.setText(task.getException().getMessage()));
        new Thread(task).start();
    }
    private HBox buildPaginationBar() {
        pageSizeBox.getSelectionModel().select(Integer.valueOf(DataBrowserController.DEFAULT_PAGE_SIZE));
        pageSizeBox.setOnAction(e -> changePageSize());

        prevPageBtn.setOnAction(e -> goToPreviousPage());
        nextPageBtn.setOnAction(e -> goToNextPage());
        loadFullTableBtn.setOnAction(e -> loadFullTableAsync());

        HBox bar = new HBox(10,
                new Label("Rows per page:"), pageSizeBox,
                prevPageBtn, paginationLabel, nextPageBtn,
                loadFullTableBtn
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 6, 0));
        return bar;
    }
    private void addFilterRow() {
        FilterRowControls row = new FilterRowControls();
        filterRows.add(row);
        filterRowsBox.getChildren().add(row.container);
    }

    private void removeFilterRow(FilterRowControls row) {
        filterRows.remove(row);
        filterRowsBox.getChildren().remove(row.container);
    }

    private void resetFilterRowsUI() {
        filterRows.clear();
        filterRowsBox.getChildren().clear();
        addFilterRow();
    }

    private void applyFilters() {
        List<FilterCondition> conditions = filterRows.stream()
                .filter(FilterRowControls::isComplete)
                .map(FilterRowControls::toCondition)
                .toList();

        if (conditions.isEmpty()) {
            statusLabel.setText("Add at least one complete filter condition.");
            return;
        }

        FilterCombinator combinator = filterCombinatorBox.getValue() == null ? FilterCombinator.AND : filterCombinatorBox.getValue();
        try {
            DataTable result = controller.applyFilters(conditions, combinator);
            renderTable(result);
            statusLabel.setText(result.getRowCount() + " rows after filter.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void clearFilters() {
        resetFilterRowsUI();
        try {
            DataTable result = controller.resetFilter();
            renderTable(result);
            statusLabel.setText(result.getRowCount() + " rows.");
        } catch (IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

}