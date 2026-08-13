package dataexploreapp.pages;
import dataexploreapp.controllers.DataBrowserController;
import dataexploreapp.db_config.database.ForeignKeyInfo;
import dataexploreapp.dialogs.ExportDialog;
import dataexploreapp.dialogs.LoginDialog;
import dataexploreapp.dialogs.ProceduresParamsDialog;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.database.ProcedureParameter;
import dataexploreapp.auth.AuthService;

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

import java.util.*;

public class DataBrowserPage {

    private final String username;
    private final DataBrowserController controller;

    private final ListView<String> tableList = new ListView<>();
    private final TableView<ObservableList<Object>> resultsTable = new TableView<>();
    private final ListView<String> procedureList = new ListView<>();

    private final ComboBox<String> filterColumnBox = new ComboBox<>();
    private final TextField filterValueField = new TextField();
    private final ComboBox<String> sortColumnBox = new ComboBox<>();
    private final ComboBox<String> sortDirectionBox =
            new ComboBox<>(FXCollections.observableArrayList("Ascending", "Descending"));

    private final ComboBox<ChartBuilder.ChartType> chartTypeBox =
            new ComboBox<>(FXCollections.observableArrayList(ChartBuilder.ChartType.values()));
    private final ComboBox<String> chartCategoryBox = new ComboBox<>();
    private final ComboBox<String> chartValueBox = new ComboBox<>();
    private final VBox columnsBox = new VBox(5);
    private final Map<String, CheckBox> columnCheckBoxes = new LinkedHashMap<>();
    private final Label statusLabel = new Label();
    private final BorderPane chartArea = new BorderPane();
    private final VBox foreignKeysBox = new VBox(6);

    private Stage stage;

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

        VBox.setVgrow(fkScroll, Priority.ALWAYS);

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

        VBox.setVgrow(columnsScroll, Priority.ALWAYS);


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


        // CENTER PANEL
        VBox centerPanel = new VBox(
                10,
                resultsTable,
                chartArea
        );

        resultsTable.setMinHeight(400);
        chartArea.setMinHeight(350);

        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        VBox.setVgrow(chartArea, Priority.ALWAYS);

        centerPanel.setPadding(new Insets(10));


        BorderPane root = new BorderPane();

        root.setTop(buildNavbar());
        root.setLeft(leftPanel);
        root.setRight(buildControlsPanel());
        root.setCenter(centerPanel);

        BorderPane.setMargin(centerPanel, new Insets(10));


        // SCENE
        Scene scene = new Scene(root, 1400, 800);

        var cssUrl = getClass().getResource("welcome.css");

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

        Button importBtn = new Button("Import file");
        importBtn.setStyle(navBtnStyle);
        importBtn.setOnAction(e -> chooseAndImportFile());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(navBtnStyle);
        logoutBtn.setOnAction(e -> {
            new LoginDialog(new AuthService()).show(new Stage());
            stage.close();
        });

        navbar.getChildren().addAll(navLabel, spacer, historyBtn, importBtn, exportBtn, logoutBtn);
        return navbar;
    }

    private VBox buildControlsPanel() {
        Button applyFilterBtn = new Button("Apply filter");
        applyFilterBtn.setOnAction(e -> applyFilter());
        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setOnAction(e -> resetFilter());
        filterValueField.setPromptText("Contains...");

        sortDirectionBox.getSelectionModel().selectFirst();
        Button applySortBtn = new Button("Apply sort");
        applySortBtn.setOnAction(e -> applySort());

        chartTypeBox.getSelectionModel().selectFirst();
        Button showChartBtn = new Button("Show chart");
        showChartBtn.getStyleClass().add("primary-button");
        showChartBtn.setOnAction(e -> renderChart());

        Button saveChartBtn = new Button("Save chart as image");
        saveChartBtn.setOnAction(e -> saveChartAsImage());

        VBox panel = new VBox(10,
                new Label("Filter"), filterColumnBox, filterValueField, new HBox(8, applyFilterBtn, clearFilterBtn),
                new Separator(),
                new Label("Sort"), sortColumnBox, sortDirectionBox, applySortBtn,
                new Separator(),
                new Label("Chart"), chartTypeBox, chartCategoryBox, chartValueBox, showChartBtn, saveChartBtn,
                new Separator(),
                statusLabel
        );
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(240);
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
            statusLabel.setText("Loaded " + controller.getCurrentTable().getRowCount() + " rows.");
        }
    }

    private void applyReapplyOutcome(DataBrowserController.ReapplyOutcome outcome) {
        if (outcome.hasFilter()) {
            filterColumnBox.setValue(outcome.filterColumn());
            filterValueField.setText(outcome.filterValue());
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

    // --- Filter / sort / chart: UI events, delegate to controller synchronously -----
    // (These operate on already-loaded in-memory data — no DB round trip, so no
    // Task/threading is needed; the controller call returns immediately.)

    private void applyFilter() {
        String column = filterColumnBox.getValue();
        String value = filterValueField.getText();
        if (column == null || value == null || value.isBlank()) return;
        try {
            DataTable result = controller.applyFilter(column, value);
            renderTable(result);
            statusLabel.setText(result.getRowCount() + " rows after filter.");
        } catch (IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void resetFilter() {
        try {
            DataTable result = controller.resetFilter();
            filterValueField.clear();
            renderTable(result);
            statusLabel.setText(result.getRowCount() + " rows.");
        } catch (IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

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
        if (table == null) return;
        String category = chartCategoryBox.getValue();
        String value = chartValueBox.getValue();
        if (category == null || value == null) return;

        Chart chart = ChartBuilder.build(table, chartTypeBox.getValue(), category, value);
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

        boolean filterActive = !filterValueField.getText().isBlank();
        String filterColumn = filterActive ? filterColumnBox.getValue() : null;
        String filterValue = filterActive ? filterValueField.getText() : null;
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
        ObservableList<String> columns = FXCollections.observableArrayList(original.getColumnNames());

        filterColumnBox.setItems(columns);
        sortColumnBox.setItems(columns);
        chartCategoryBox.setItems(columns);
        chartValueBox.setItems(columns);

        if (!columns.isEmpty()) {
            filterColumnBox.getSelectionModel().selectFirst();
            sortColumnBox.getSelectionModel().selectFirst();
            chartCategoryBox.getSelectionModel().selectFirst();
            chartValueBox.getSelectionModel().selectFirst();
        }

        columnsBox.getChildren().clear();
        columnCheckBoxes.clear();
        for (String columnName : columns) {
            CheckBox checkBox = new CheckBox(columnName);
            checkBox.setSelected(true);
            checkBox.setOnAction(e -> updateVisibleColumns());
            columnCheckBoxes.put(columnName, checkBox);
            columnsBox.getChildren().add(checkBox);
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
}