package dataexploreapp.pages;

import dataexploreapp.history.ExportHistoryService;
import dataexploreapp.history.ExportRecord;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;


public class ExportHistoryPage {

    private final DataBrowserPage originatingBrowser;
    private TableView<ExportRecord> table;

    public ExportHistoryPage(DataBrowserPage originatingBrowser) {
        this.originatingBrowser = originatingBrowser;
    }

    public void show(Stage stage) {
        table = new TableView<>();

        TableColumn<ExportRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<ExportRecord, String> tableCol = new TableColumn<>("Table");
        tableCol.setCellValueFactory(new PropertyValueFactory<>("tableName"));

        TableColumn<ExportRecord, String> formatCol = new TableColumn<>("Format");
        formatCol.setCellValueFactory(new PropertyValueFactory<>("format"));

        TableColumn<ExportRecord, String> filterCol = new TableColumn<>("Filter");
        filterCol.setCellValueFactory(cell -> {
            ExportRecord r = cell.getValue();
            String text = (r.getFilterColumn() == null || r.getFilterValue() == null || r.getFilterValue().isBlank())
                    ? "—"
                    : r.getFilterColumn() + " contains \"" + r.getFilterValue() + "\"";
            return new SimpleStringProperty(text);
        });

        TableColumn<ExportRecord, String> sortCol = new TableColumn<>("Sort");
        sortCol.setCellValueFactory(cell -> {
            ExportRecord r = cell.getValue();
            String text = r.getSortColumn() == null
                    ? "—"
                    : r.getSortColumn() + " (" + (Boolean.TRUE.equals(r.getSortAscending()) ? "asc" : "desc") + ")";
            return new SimpleStringProperty(text);
        });

        TableColumn<ExportRecord, String> rowsCol = new TableColumn<>("Rows");
        rowsCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getRowCount())));

        TableColumn<ExportRecord, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("outputPath"));

        TableColumn<ExportRecord, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button reapplyBtn = new Button("Reapply filters");
            {
                reapplyBtn.setOnAction(e -> {
                    ExportRecord record = getTableView().getItems().get(getIndex());
                    originatingBrowser.applyHistoryRecord(record);
                    ((Stage) getScene().getWindow()).close();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : reapplyBtn);
            }
        });

        table.getColumns().addAll(List.of(dateCol, tableCol, formatCol, filterCol, sortCol, rowsCol, fileCol, actionCol));

        refreshTable();
        table.setPlaceholder(new Label("No exports yet."));

        Label title = new Label("Export history");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearHistoryBtn = new Button("Clear history");
        clearHistoryBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-cursor: hand;");
        clearHistoryBtn.setOnAction(e -> confirmAndClearHistory());

        HBox header = new HBox(12, title, spacer, clearHistoryBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12, header, table);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 500);
        var cssUrl = getClass().getResource("global.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setTitle("Export history");
        stage.setScene(scene);
        stage.show();
    }

    private void confirmAndClearHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear export history");
        confirm.setHeaderText("Delete all export history?");
        confirm.setContentText("This can't be undone. Your actual exported files won't be affected — only the history list.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ExportHistoryService.clear();
            refreshTable();
        }
    }

    private void refreshTable() {
        List<ExportRecord> history = ExportHistoryService.loadHistory();
        table.setItems(FXCollections.observableArrayList(history));
    }
}