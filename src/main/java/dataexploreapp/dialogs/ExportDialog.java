package dataexploreapp.dialogs;

import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.dataexport.ExportFormat;
import dataexploreapp.history.ExportHistoryService;
import dataexploreapp.history.ExportRecord;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ExportDialog {

    private final DataTable dataTable;
    private final String tableName;
    private final boolean isProcedure;
    private final String filterColumn;
    private final String filterValue;
    private final String sortColumn;
    private final Boolean sortAscending;

    private final ComboBox<ExportFormat> formatBox = new ComboBox<>();
    private final Label statusLabel = new Label();
    private File selectedFile;
    private Stage stage;
    private String filePath;

    public ExportDialog(DataTable dataTable, String tableName, boolean isProcedure, String filePath,
                        String filterColumn, String filterValue,
                        String sortColumn, Boolean sortAscending) {
        this.dataTable = dataTable;
        this.tableName = tableName;
        this.isProcedure = isProcedure;
        this.filePath = filePath;
        this.filterColumn = filterColumn;
        this.filterValue = filterValue;
        this.sortColumn = sortColumn;
        this.sortAscending = sortAscending;
    }

    public void show(Stage stage) {
        this.stage = stage;

        Label title = new Label("Export " + dataTable.getRowCount() + " rows");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        statusLabel.getStyleClass().add("status-label");
        formatBox.setId("formatBox");

        formatBox.getItems().addAll(ExportFormat.values());
        formatBox.getSelectionModel().selectFirst();

        Button chooseFileBtn = new Button("Choose output file...");
        chooseFileBtn.setOnAction(e -> chooseFile());
        chooseFileBtn.setId("chooseFileButton");
        Button exportBtn = new Button("Export");
        exportBtn.getStyleClass().add("primary-button");
        exportBtn.setOnAction(e -> runExport());
        exportBtn.setId("exportButton");
        VBox root = new VBox(12, title, new Label("Format:"), formatBox, chooseFileBtn, exportBtn, statusLabel);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 360, 280);
        dataexploreapp.themes.ThemeManager.register(scene);
        stage.setTitle("Export data");
        stage.setScene(scene);
        stage.show();
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        ExportFormat format = formatBox.getValue();
        String defaultName = dataTable.getTitle().replaceAll("[^a-zA-Z0-9_-]", "_") + format.getExtension();
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(format.toString(), "*" + format.getExtension())
        );

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(format.getExtension())) {
                path += format.getExtension();
            }
            selectedFile = new File(path);
            statusLabel.setText("Selected: " + selectedFile.getName());
        }
    }

    private void runExport() {
        if (selectedFile == null) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Choose an output file first.");
            return;
        }

        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setText("Exporting...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                formatBox.getValue().getExporter().export(dataTable, selectedFile.toPath());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Done: " + selectedFile.getName());
            recordHistory();
        });

        task.setOnFailed(e -> {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Export failed: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void recordHistory() {
        ExportRecord record = new ExportRecord(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                tableName,
                isProcedure,
                filePath,
                formatBox.getValue().toString(),
                filterColumn,
                filterValue,
                sortColumn,
                sortAscending,
                selectedFile.getAbsolutePath(),
                dataTable.getRowCount()
        );
        ExportHistoryService.append(record);
    }
}