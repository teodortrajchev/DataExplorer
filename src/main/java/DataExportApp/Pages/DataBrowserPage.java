package DataExportApp.Pages;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import Config.Database.DataBaseReader;
import Config.Database.DataTable;
import Config.Database.ProcedureParameter;
import DataExportApp.Auth.AuthService;
import DataExportApp.Import.DataImportService;
import java.io.File;
import java.io.IOException;

import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import DataExportApp.Charts.ChartBuilder;
import DataExportApp.History.ExportRecord;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class DataBrowserPage {

    private final String username;
    private final DataBaseReader reader;

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
    private String currentImportedFilePath;
    private DataTable originalTable;
    private DataTable currentTable;
    private boolean currentIsProcedure;
    private Stage stage;

    private final String schema;

    public DataBrowserPage(String username, DataBaseReader reader, String schema) {
        this.username = username;
        this.reader = reader;
        this.schema = schema;
    }

    public void show(Stage stage) {
        this.stage = stage;

        VBox tablesPanel = new VBox(8, new Label("Tables"), tableList);
        tablesPanel.setPadding(new Insets(10));
        tablesPanel.setPrefWidth(200);

        tableList.setOnMouseClicked(e -> {
            String selected = tableList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                loadTable(selected, null);
            }
        });

        VBox proceduresPanel = new VBox(8, new Label("Procedures"), procedureList);
        proceduresPanel.setPadding(new Insets(10));
        proceduresPanel.setPrefWidth(200);

        procedureList.setOnMouseClicked(e -> {
            String selected = procedureList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                promptAndLoadProcedure(selected, null);
            }
        });

// Columns panel
        columnsBox.setPadding(new Insets(10));
        columnsBox.setPrefWidth(220);

        ScrollPane columnsScroll = new ScrollPane(columnsBox);
        columnsScroll.setFitToWidth(true);
        columnsScroll.setPrefWidth(220);
        columnsScroll.setPrefHeight(400);

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
                new Label("Columns"),
                columnButtons,
                columnsScroll
        );
        columnsPanel.setPadding(new Insets(10));
        columnsPanel.setPrefWidth(240);


// Tables + Columns
        HBox leftTopPanel = new HBox(
                10,
                tablesPanel,
                columnsPanel
        );

        VBox leftPanel = new VBox(
                10,
                leftTopPanel,
                proceduresPanel
        );

        VBox centerPanel = new VBox(10, resultsTable, chartArea);
        resultsTable.setPrefHeight(300);
        chartArea.setPrefHeight(300);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        VBox.setVgrow(chartArea, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(buildNavbar());
        root.setLeft(leftPanel);
        root.setRight(buildControlsPanel());
        root.setCenter(centerPanel);
        BorderPane.setMargin(centerPanel, new Insets(10));

        Scene scene = new Scene(root, 1400, 800);
        var cssUrl = getClass().getResource("welcome.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Data Browser");
        stage.setScene(scene);
        stage.show();

        loadTableList();
        loadProceduresList();
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
            new LoginView(new AuthService()).show(new Stage());
            stage.close();
        });

        navbar.getChildren().addAll(navLabel, spacer, historyBtn,importBtn, exportBtn, logoutBtn);
        return navbar;
    }

    private VBox buildControlsPanel() {
        Button applyFilterBtn = new Button("Apply filter");
        applyFilterBtn.setOnAction(e -> applyFilter());
        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setOnAction(e -> resetToOriginal());
        filterValueField.setPromptText("Contains...");

        sortDirectionBox.getSelectionModel().selectFirst();
        Button applySortBtn = new Button("Apply sort");
        applySortBtn.setOnAction(e -> applySort());

        chartTypeBox.getSelectionModel().selectFirst();
        Button showChartBtn = new Button("Show chart");
        showChartBtn.getStyleClass().add("primary-button");
        showChartBtn.setOnAction(e -> renderChart());
        chartTypeBox.getSelectionModel().selectFirst();


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

    private void loadTableList() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                return reader.listTables(schema);
            }
        };
        task.setOnSucceeded(e -> tableList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> statusLabel.setText("Failed to list tables: " + task.getException().getMessage()));
        new Thread(task).start();
    }
    private void loadProceduresList() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                return reader.listProcedures(schema);
            }
        };
        task.setOnSucceeded(e -> procedureList.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(e -> statusLabel.setText("Failed to list tables: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void loadTable(String tableName, ExportRecord historyRecord) {
        statusLabel.setText("Loading " + tableName + "...");
        Task<DataTable> task = new Task<>() {
            @Override
            protected DataTable call() throws SQLException {
                return reader.loadTable(schema, tableName);
            }
        };
        task.setOnSucceeded(e -> {
            originalTable = task.getValue();
            currentTable = originalTable;
            currentIsProcedure = false;
            currentImportedFilePath = null;
            populateColumnPickers();

            if (historyRecord != null) {
                reapplyRecord(historyRecord);
            } else {
                renderTable(currentTable);
                statusLabel.setText("Loaded " + currentTable.getRowCount() + " rows.");
            }
        });
        task.setOnFailed(e -> {
            String message = task.getException().getMessage();
            // ORA-04044: not a table — likely a procedure, especially for older
            // history entries saved before isProcedure was tracked. Retry as a procedure.
            if (message != null && message.contains("ORA-04044")) {
                loadProcedure(tableName, Map.of(), historyRecord);
            } else {
                statusLabel.setText("Failed to load table: " + message);
            }
        });
        new Thread(task).start();
    }

    private void reapplyRecord(ExportRecord record) {
        boolean hasFilter = record.getFilterColumn() != null
                && record.getFilterValue() != null
                && !record.getFilterValue().isBlank();
        boolean hasSort = record.getSortColumn() != null;

        if (hasFilter) {
            filterColumnBox.setValue(record.getFilterColumn());
            filterValueField.setText(record.getFilterValue());
            applyFilter();
        }
        if (hasSort) {
            sortColumnBox.setValue(record.getSortColumn());
            sortDirectionBox.setValue(Boolean.TRUE.equals(record.getSortAscending()) ? "Ascending" : "Descending");
            applySort();
        }

        if (!hasFilter && !hasSort) {
            renderTable(currentTable);
            statusLabel.setText("This export had no filter or sort — loaded full table (" + currentTable.getRowCount() + " rows).");
        } else {
            statusLabel.setText("Reapplied " + (hasFilter ? "filter" : "") + (hasFilter && hasSort ? " + " : "") + (hasSort ? "sort" : "") + " from history.");
        }
    }

    public void applyHistoryRecord(ExportRecord record) {
        stage.toFront();
        stage.requestFocus();

        if (record.getFilePath() != null) {
            File file = new File(record.getFilePath());
            if (!file.exists()) {
                statusLabel.setText("Original file no longer exists: " + record.getFilePath());
                return;
            }
            loadFile(file, record);
        } else if (record.isProcedure()) {
            procedureList.getSelectionModel().select(record.getTableName());
            loadProcedure(record.getTableName(), Map.of(), record);
        } else {
            tableList.getSelectionModel().select(record.getTableName());
            loadTable(record.getTableName(), record);
        }
    }

    private void promptAndLoadProcedure(String procedureName, ExportRecord historyRecord) {
        statusLabel.setText("Checking parameters for " + procedureName + "...");
        Task<List<ProcedureParameter>> paramsTask = new Task<>() {
            @Override
            protected List<ProcedureParameter> call() throws SQLException {
                return reader.getProcedureParameters(schema, procedureName);
            }
        };
        paramsTask.setOnSucceeded(e -> {
            List<ProcedureParameter> inputParams = paramsTask.getValue().stream()
                    .filter(ProcedureParameter::isInput)
                    .toList();

            if (inputParams.isEmpty()) {
                loadProcedure(procedureName, Map.of(), historyRecord);
            } else {
                Optional<Map<String, String>> values = ProceduresParamsDialog.show(stage, procedureName, inputParams);
                if (values.isPresent()) {
                    loadProcedure(procedureName, values.get(), historyRecord);
                } else {
                    statusLabel.setText("Cancelled.");
                }
            }
        });
        paramsTask.setOnFailed(e -> statusLabel.setText("Failed to check parameters: " + paramsTask.getException().getMessage()));
        new Thread(paramsTask).start();
    }

    private void loadProcedure(String procedureName, Map<String, String> paramValues, ExportRecord historyRecord) {
        statusLabel.setText("Loading " + procedureName + "...");
        Task<DataTable> task = new Task<>() {
            @Override
            protected DataTable call() throws SQLException {
                return reader.loadProcedure(schema, procedureName, paramValues);
            }
        };
        task.setOnSucceeded(e -> {
            originalTable = task.getValue();
            currentTable = originalTable;
            currentIsProcedure = true;
            currentImportedFilePath = null;
            populateColumnPickers();

            if (historyRecord != null) {
                reapplyRecord(historyRecord);
            } else {
                renderTable(currentTable);
                statusLabel.setText("Loaded " + currentTable.getRowCount() + " rows.");
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to load procedure: " + task.getException().getMessage()));
        new Thread(task).start();
    }
    private void updateVisibleColumns() {
        if (currentTable == null) {
            return;
        }

        renderTable(currentTable);
    }
    private void populateColumnPickers() {
        ObservableList<String> columns =
                FXCollections.observableArrayList(originalTable.getColumnNames());

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

        // Create column checkboxes
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


    private void applyFilter() {
        if (originalTable == null) return;
        String column = filterColumnBox.getValue();
        String value = filterValueField.getText();
        if (column == null || value == null || value.isBlank()) return;

        currentTable = currentTable.filterContains(column, value);
        renderTable(currentTable);
        statusLabel.setText(currentTable.getRowCount() + " rows after filter.");
    }

    private void resetToOriginal() {
        if (originalTable == null) return;
        currentTable = originalTable;
        filterValueField.clear();
        renderTable(currentTable);
        statusLabel.setText(currentTable.getRowCount() + " rows.");
    }

    private void applySort() {
        if (currentTable == null) return;
        String column = sortColumnBox.getValue();
        if (column == null) return;
        currentTable = currentTable.sortBy(column, "Ascending".equals(sortDirectionBox.getValue()));
        renderTable(currentTable);
    }


    private void renderChart() {
        if (currentTable == null) return;
        String category = chartCategoryBox.getValue();
        String value = chartValueBox.getValue();
        if (category == null || value == null) return;

        Chart chart = ChartBuilder.build(currentTable, chartTypeBox.getValue(), category, value);
        chartArea.setCenter(chart);
    }

    private void openExportDialog() {
        if (currentTable == null) {
            statusLabel.setText("Load a table first.");
            return;
        }

        boolean filterActive = !filterValueField.getText().isBlank();
        String filterColumn = filterActive ? filterColumnBox.getValue() : null;
        String filterValue = filterActive ? filterValueField.getText() : null;
        String sortColumn = sortColumnBox.getValue();
        Boolean sortAscending = sortColumn == null ? null : "Ascending".equals(sortDirectionBox.getValue());

        new ExportDialog(currentTable, currentTable.getTitle(), currentIsProcedure, currentImportedFilePath,
                filterColumn, filterValue, sortColumn, sortAscending)
                .show(new Stage());
    }
    private void renderTable(DataTable dataTable) {
        resultsTable.getColumns().clear();
        List<String> columns = dataTable.getColumnNames();

        for (int i = 0; i < columns.size(); i++) {

            String columnName = columns.get(i);
            CheckBox checkBox = columnCheckBoxes.get(columnName);
            // Skip unchecked columns
            if (checkBox != null && !checkBox.isSelected()) {
                continue;
            }
            final int colIndex = i;

            TableColumn<ObservableList<Object>, Object> column = new TableColumn<>(columnName);

            column.setCellValueFactory(
                    cell -> new SimpleObjectProperty<>(cell.getValue().get(colIndex))
            );
            resultsTable.getColumns().add(column);
        }
        ObservableList<ObservableList<Object>> rows = FXCollections.observableArrayList();
        for (Object[] row : dataTable.getRows()) {
            rows.add(FXCollections.observableArrayList(row));
        }
        resultsTable.setItems(rows);
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
            loadFile(file, null);
        }
    }

    private void loadFile(File file, ExportRecord historyRecord) {
        statusLabel.setText("Importing " + file.getName() + "...");
        Task<DataTable> task = new Task<>() {
            @Override
            protected DataTable call() throws IOException {
                return DataImportService.importFile(file);
            }
        };
        task.setOnSucceeded(e -> {
            originalTable = task.getValue();
            currentTable = originalTable;
            currentIsProcedure = false;
            currentImportedFilePath = file.getAbsolutePath();
            populateColumnPickers();

            if (historyRecord != null) {
                reapplyRecord(historyRecord);
            } else {
                renderTable(currentTable);
                statusLabel.setText("Imported " + currentTable.getRowCount() + " rows from " + file.getName() + ".");
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Import failed: " + task.getException().getMessage()));
        new Thread(task).start();
    }
    private void saveChartAsImage() {
        if (chartArea.getCenter() == null) {
            statusLabel.setText("Show a chart first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save chart as image");
        chooser.setInitialFileName(currentTable != null ? currentTable.getTitle() + "_chart.png" : "chart.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        // Snapshot with a transparent-safe white background, since charts
        // otherwise export with a see-through background that looks broken
        // when opened outside the app.
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);

        WritableImage snapshot = chartArea.getCenter().snapshot(params, null);

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
            statusLabel.setText("Saved chart: " + file.getName());
        } catch (IOException ex) {
            statusLabel.setText("Failed to save chart: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}