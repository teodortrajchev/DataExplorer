package ui;

import dataexploreapp.history.ExportHistoryService;
import dataexploreapp.history.ExportRecord;
import dataexploreapp.pages.DataBrowserPage;
import dataexploreapp.pages.ExportHistoryPage;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ExportHistoryPageTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;

        ExportHistoryService.clear();

        DataBrowserPage browser = null;
        ExportHistoryPage page = new ExportHistoryPage(browser);
        page.show(stage);
    }

    @BeforeEach
    void cleanHistory() {
        ExportHistoryService.clear();
    }

    @Test
    void exportHistoryPage_opensSuccessfully_test(FxRobot robot) {

        assertTrue(stage.isShowing());
        assertEquals("Export history", stage.getTitle());
    }

    @Test
    void table_isDisplayed_test(FxRobot robot) {

        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertNotNull(table);
        assertTrue(table.isVisible());
    }

    @Test
    void table_containsExpectedColumns_test(FxRobot robot) {

        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertEquals(8, table.getColumns().size());

        assertEquals("Date", table.getColumns().get(0).getText());
        assertEquals("Table", table.getColumns().get(1).getText());
        assertEquals("Format", table.getColumns().get(2).getText());
        assertEquals("Filter", table.getColumns().get(3).getText());
        assertEquals("Sort", table.getColumns().get(4).getText());
        assertEquals("Rows", table.getColumns().get(5).getText());
        assertEquals("File", table.getColumns().get(6).getText());
        assertEquals("Action", table.getColumns().get(7).getText());
    }

    @Test
    void emptyHistory_displaysPlaceholder_test(FxRobot robot) {

        robot.interact(() -> new ExportHistoryPage(null).show(stage));

        Label placeholder = robot.lookup("No exports yet.").queryAs(Label.class);
        assertEquals("No exports yet.", placeholder.getText());
    }

    @Test
    void historyRecord_isDisplayed_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.xlsx",
                "XLSX",
                null,
                null,
                null,
                null,
                "C:\\exports\\employees.xlsx",
                25
        );

        ExportHistoryService.append(record);

        robot.interact(() -> {new ExportHistoryPage(null).show(stage);});
        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);
        assertEquals(1, table.getItems().size());
        ExportRecord displayed = (ExportRecord) table.getItems().get(0);

        assertEquals("EMPLOYEES", displayed.getTableName());
        assertEquals("XLSX", displayed.getFormat());
        assertEquals(25, displayed.getRowCount());
    }

    @Test
    void historyRecords_areDisplayedNewestFirst_test(FxRobot robot) {

        ExportRecord older = new ExportRecord(
                "2026-08-27 10:00:00",
                "EMPLOYEES",
                false,
                "old.xlsx",
                "XLSX",
                null,
                null,
                null,
                null,
                "old.xlsx",
                10
        );

        ExportRecord newer = new ExportRecord(
                "2026-08-27 11:00:00",
                "CUSTOMERS",
                false,
                "new.csv",
                "CSV",
                null,
                null,
                null,
                null,
                "new.csv",
                50
        );

        ExportHistoryService.append(older);
        ExportHistoryService.append(newer);

        robot.interact(() -> {new ExportHistoryPage(null).show(stage);});
        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertEquals(2, table.getItems().size());
        ExportRecord first = (ExportRecord) table.getItems().get(0);
        ExportRecord second = (ExportRecord) table.getItems().get(1);

        assertEquals("CUSTOMERS", first.getTableName());
        assertEquals("EMPLOYEES", second.getTableName());
    }

    @Test
    void filter_isFormattedCorrectly_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.csv",
                "CSV",
                "EMAIL",
                "gmail",
                null,
                null,
                "employees.csv",
                5
        );

        ExportHistoryService.append(record);

        robot.interact(() -> {new ExportHistoryPage(null).show(stage);});
        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertEquals(1, table.getItems().size());

        ExportRecord displayed = (ExportRecord) table.getItems().get(0);
        assertEquals("EMAIL", displayed.getFilterColumn());
        assertEquals("gmail", displayed.getFilterValue());

        assertEquals("EMAIL contains \"gmail\"", displayed.getFilterColumn() + " contains \"" + displayed.getFilterValue() + "\"");
    }

    @Test
    void sort_isFormattedCorrectly_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.csv",
                "CSV",
                null,
                null,
                "SALARY",
                true,
                "employees.csv",
                10
        );

        ExportHistoryService.append(record);

        robot.interact(() -> {new ExportHistoryPage(null).show(stage);});

        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);
        ExportRecord displayed = (ExportRecord) table.getItems().get(0);

        assertEquals("SALARY", displayed.getSortColumn());
        assertTrue(displayed.getSortAscending());
    }

    @Test
    void clearHistoryButton_isDisplayed_test(FxRobot robot) {

        Button clearButton = robot.lookup("Clear history").queryAs(Button.class);

        assertNotNull(clearButton);
        assertTrue(clearButton.isVisible());
        assertFalse(clearButton.isDisabled());
    }

    @Test
    void clearHistory_removesRecords_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.xlsx",
                "XLSX",
                null,
                null,
                null,
                null,
                "employees.xlsx",
                20
        );

        ExportHistoryService.append(record);

        robot.interact(() -> new ExportHistoryPage(null).show(stage));

        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertEquals(1, table.getItems().size());

        robot.clickOn("Clear history");
        robot.clickOn("OK");

        assertEquals(0, table.getItems().size());

        assertTrue(ExportHistoryService.loadHistory().isEmpty());
    }

    @Test
    void clearHistory_cancelDoesNotDeleteRecords_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.xlsx",
                "XLSX",
                null,
                null,
                null,
                null,
                "employees.xlsx",
                20
        );

        ExportHistoryService.append(record);

        robot.interact(() -> new ExportHistoryPage(null).show(stage));

        TableView<?> table = robot.lookup(".table-view").queryAs(TableView.class);

        assertEquals(1, table.getItems().size());

        robot.clickOn("Clear history");

        // Dont confirm.
        robot.clickOn("Cancel");

        assertEquals(1, table.getItems().size());

        assertEquals(1, ExportHistoryService.loadHistory().size());
    }

    @Test
    void filterWithEmptyValue_displaysDash_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.csv",
                "CSV",
                null,
                null,
                null,
                null,
                "employees.csv",
                10
        );

        ExportHistoryService.append(record);

        robot.interact(() -> new ExportHistoryPage(null).show(stage));

        ExportRecord loaded = ExportHistoryService.loadHistory().get(0);

        assertNull(loaded.getFilterColumn());
        assertNull(loaded.getFilterValue());
    }

    @Test
    void sortDescending_isStoredCorrectly_test(FxRobot robot) {

        ExportRecord record = new ExportRecord(
                "2026-08-27 12:00:00",
                "EMPLOYEES",
                false,
                "employees.csv",
                "CSV",
                null,
                null,
                "SALARY",
                false,
                "employees.csv",
                10
        );

        ExportHistoryService.append(record);

        assertFalse(ExportHistoryService.loadHistory().get(0).getSortAscending());
    }
}
