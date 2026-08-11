package DataExportApp.Pages;

import DataExportApp.History.ExportHistoryService;
import DataExportApp.History.ExportRecord;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;


public class ExportHistoryPage {

    private final DataBrowserPage originatingBrowser;

    public ExportHistoryPage(DataBrowserPage originatingBrowser) {
        this.originatingBrowser = originatingBrowser;
    }

    public void show(Stage stage) {
        TableView<ExportRecord> table = new TableView<>();

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

        List<ExportRecord> history = ExportHistoryService.loadHistory();
        table.setItems(FXCollections.observableArrayList(history));
        table.setPlaceholder(new Label("No exports yet."));

        Label title = new Label("Export history");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox root = new VBox(12, title, table);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 500);
        stage.setTitle("Export history");
        stage.setScene(scene);
        stage.show();
    }
}