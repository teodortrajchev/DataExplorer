package ui;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.pages.DataBrowserPage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(ApplicationExtension.class)
class DataBrowserPageTest {

    private static final String SCHEMA = "SRB";

    private DataBaseReader reader;
    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        this.reader = mock(DataBaseReader.class);

        try {
            when(reader.listTables(SCHEMA)).thenReturn(List.of());
            when(reader.listProcedures(SCHEMA)).thenReturn(List.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new DataBrowserPage("testuser", reader, SCHEMA).show(stage);
    }

    @BeforeEach
    void bringToFront(FxRobot robot) {
        robot.interact(() -> stage.toFront());
    }

    @Test
    void onShow_loadsTableAndProcedureListsFromReader(FxRobot robot) throws Exception {
        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES", "DEPARTMENTS"));
        when(reader.listProcedures(SCHEMA)).thenReturn(List.of("GET_REPORT"));

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().size() == 2 && !procedureList(robot).getItems().isEmpty());

        assertEquals(List.of("EMPLOYEES", "DEPARTMENTS"), tableList(robot).getItems());
        assertEquals(List.of("GET_REPORT"), procedureList(robot).getItems());
    }

    @Test
    void selectingTable_loadsAndRendersItsData(FxRobot robot) throws Exception {
        DataTable table = new DataTable(List.of("ID", "NAME"));
        table.addRow(new Object[]{1, "Alice"});
        table.addRow(new Object[]{2, "Bob"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));
        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(2L);
        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));
        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(table);
        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        // Reload the page now that the mock is stubbed the way this test needs.
        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> "Loaded 2 rows.".equals(statusLabel(robot).getText()));

        assertEquals("Loaded 2 rows.", statusLabel(robot).getText());
        assertEquals(2, resultsTable(robot).getItems().size());
        assertEquals(2, resultsTable(robot).getColumns().size());
    }

    @Test
    void selectingTable_whenLoadFails_showsFailureStatus(FxRobot robot) throws Exception {
        when(reader.listTables(SCHEMA)).thenReturn(List.of("BROKEN_TABLE"));
        when(reader.getRowCount(SCHEMA, "BROKEN_TABLE")).thenThrow(new java.sql.SQLException("ORA-00942: table or view does not exist"));

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("BROKEN_TABLE"));

        robot.clickOn("BROKEN_TABLE");

        waitUntil(robot, () -> statusLabel(robot).getText().startsWith("Failed to load table:"));

        assertTrue(statusLabel(robot).getText().contains("ORA-00942"));
    }




    @SuppressWarnings("unchecked")
    private ListView<String> tableList(FxRobot robot) {
        return robot.lookup("#tableList").queryAs(ListView.class);
    }

    @SuppressWarnings("unchecked")
    private ListView<String> procedureList(FxRobot robot) {
        return robot.lookup("#procedureList").queryAs(ListView.class);
    }

    private Label statusLabel(FxRobot robot) {
        return robot.lookup("#statusLabel").queryAs(Label.class);
    }

    @SuppressWarnings("unchecked")
    private TableView<javafx.collections.ObservableList<Object>> resultsTable(FxRobot robot) {
        return robot.lookup("#resultsTable").queryAs(TableView.class);
    }


    private void waitUntil(FxRobot robot, Supplier<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> {
                WaitForAsyncUtils.waitForFxEvents();
                return condition.get();
            });
        } catch (TimeoutException e) {
            throw new AssertionError("Condition was not met within 10 seconds", e);
        }
    }
    @Test
    void applyingAscendingSort_sortsRows(FxRobot robot) throws Exception {

        DataTable table = new DataTable(List.of("ID", "NAME"));
        table.addRow(new Object[]{3, "Charlie"});
        table.addRow(new Object[]{1, "Alice"});
        table.addRow(new Object[]{2, "Bob"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));

        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(3L);
        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));
        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(table);
        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        robot.clickOn("#sortColumnBox");
        robot.clickOn("ID");

        robot.clickOn("#sortDirectionBox");
        robot.clickOn("Ascending");

        robot.clickOn("#applySortBtn");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        assertEquals(1, resultsTable(robot).getItems().get(0).get(0));

        assertEquals(2, resultsTable(robot).getItems().get(1).get(0));

        assertEquals(3, resultsTable(robot).getItems().get(2).get(0));
    }

    @Test
    void applyingDescendingSort_sortsRows(FxRobot robot) throws Exception {

        DataTable table = new DataTable(List.of("ID", "NAME"));
        table.addRow(new Object[]{1, "Alice"});
        table.addRow(new Object[]{3, "Charlie"});
        table.addRow(new Object[]{2, "Bob"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));
        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(3L);
        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(table);
        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        robot.clickOn("#sortColumnBox");
        robot.clickOn("ID");

        robot.clickOn("#sortDirectionBox");
        robot.clickOn("Descending");

        robot.clickOn("#applySortBtn");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        assertEquals(3, resultsTable(robot).getItems().get(0).get(0));
        assertEquals(2, resultsTable(robot).getItems().get(1).get(0));
        assertEquals(1, resultsTable(robot).getItems().get(2).get(0));
    }


//pomosnik za filter dropdown
    @SuppressWarnings("unchecked")
    private ComboBox<String> filterColumnBox(FxRobot robot) {
        return robot.lookup("#filterColumn")
                .queryAs(ComboBox.class);
    }
    @Test
    void applyingFilter_showsOnlyMatchingRows(FxRobot robot) throws Exception {

        DataTable table = new DataTable(List.of("ID", "NAME"));

        table.addRow(new Object[]{1, "Alice"});
        table.addRow(new Object[]{2, "Bob"});
        table.addRow(new Object[]{3, "Alice Smith"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));

        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(3L);

        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(table);

        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () ->tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        robot.clickOn("#filterColumn");
        robot.interact(() -> filterColumnBox(robot).getSelectionModel().select("NAME"));

        robot.clickOn("#filterValue");
        robot.write("Alice");

        robot.clickOn("Apply filters");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 2);

        assertEquals(1, resultsTable(robot).getItems().get(0).get(0));

        assertEquals("Alice", resultsTable(robot).getItems().get(0).get(1));

        assertEquals(3, resultsTable(robot).getItems().get(1).get(0));

        assertEquals("Alice Smith", resultsTable(robot).getItems().get(1).get(1));
    }
    @Test
    void clearingFilter_restoresAllRows(FxRobot robot) throws Exception {

        DataTable table = new DataTable(List.of("ID", "NAME"));

        table.addRow(new Object[]{1, "Alice"});
        table.addRow(new Object[]{2, "Bob"});
        table.addRow(new Object[]{3, "Charlie"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));

        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(3L);

        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(table);

        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        robot.clickOn("#filterColumn");
        robot.interact(() -> filterColumnBox(robot).getSelectionModel().select("NAME"));

        robot.clickOn("#filterValue");
        robot.write("Alice");

        robot.clickOn("Apply filters");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 1);

        robot.clickOn("Clear");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 3);

        assertEquals(3, resultsTable(robot).getItems().size());
    }
    @Test
    void nextPage_loadsSecondPage(FxRobot robot) throws Exception {

        DataTable firstPage = new DataTable(List.of("ID", "NAME"));

        firstPage.addRow(new Object[]{1, "Employee 1"});

        firstPage.addRow(new Object[]{2, "Employee 2"});

        DataTable secondPage = new DataTable(List.of("ID", "NAME"));

        secondPage.addRow(new Object[]{101, "Employee 101"});

        secondPage.addRow(new Object[]{102, "Employee 102"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));

        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(250L);

        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(firstPage);

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 100, 100)).thenReturn(secondPage);
        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () -> resultsTable(robot).getItems().size() == 2);

        assertEquals(1, resultsTable(robot).getItems().get(0).get(0));

        robot.clickOn("#nextPageBtn");

        waitUntil(robot, () -> resultsTable(robot).getItems().get(0).get(0).equals(101));

        assertEquals(101, resultsTable(robot).getItems().get(0).get(0));

        assertEquals(102, resultsTable(robot).getItems().get(1).get(0));
    }
    @Test
    void previousPage_returnsToFirstPage(FxRobot robot) throws Exception {

        DataTable firstPage = new DataTable(List.of("ID", "NAME"));

        firstPage.addRow(new Object[]{1, "Employee 1"});

        DataTable secondPage = new DataTable(List.of("ID", "NAME"));

        secondPage.addRow(new Object[]{101, "Employee 101"});

        when(reader.listTables(SCHEMA)).thenReturn(List.of("EMPLOYEES"));

        when(reader.getRowCount(SCHEMA, "EMPLOYEES")).thenReturn(250L);

        when(reader.getColumnNames(SCHEMA, "EMPLOYEES")).thenReturn(List.of("ID", "NAME"));

        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 0, 100)).thenReturn(firstPage);
        when(reader.loadTablePage(SCHEMA, "EMPLOYEES", List.of(), "ID", 100, 100)).thenReturn(secondPage);
        when(reader.getForeignKeys(SCHEMA, "EMPLOYEES")).thenReturn(List.of());

        robot.interact(() -> new DataBrowserPage("testuser", reader, SCHEMA).show(stage));

        waitUntil(robot, () -> tableList(robot).getItems().contains("EMPLOYEES"));

        robot.clickOn("EMPLOYEES");

        waitUntil(robot, () ->resultsTable(robot).getItems().size() == 1);

        robot.clickOn("#nextPageBtn");

        waitUntil(robot, () -> resultsTable(robot).getItems().get(0).get(0).equals(101));

        robot.clickOn("#prevPageBtn");

        waitUntil(robot, () -> resultsTable(robot).getItems().get(0).get(0).equals(1));
        assertEquals(1, resultsTable(robot).getItems().get(0).get(0));
    }
}