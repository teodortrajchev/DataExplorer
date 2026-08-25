package db;

import dataexploreapp.controllers.DataBrowserController;
import dataexploreapp.db_config.database.DataBaseReader;

import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.database.ForeignKeyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataBrowserControllerTest {

    @Mock
    private DataBaseReader reader;

    private DataBrowserController controller;

    @BeforeEach
    void setUp() {
        controller = new DataBrowserController(reader, "HR");
    }

    @Test
    void listTables_test() throws SQLException {
        List<String> tables = List.of("emps", "deps");
        when(reader.listTables("HR")).thenReturn(tables);
        List<String> result = controller.listTables();
        assertEquals(tables, result);
        verify(reader).listTables("HR");
    }

    @Test
    void loadTable_test() throws SQLException {
        DataTable table = new DataTable(List.of("id", "name"));
        table.addRow(new Object[]{1, "Teodor"});
        when(reader.getRowCount("HR", "users")).thenReturn(1L);
        when(reader.getColumnNames("HR", "users")).thenReturn(List.of("id", "name"));
        when(reader.loadTablePage("HR", "users", List.of(), "id", 0, DataBrowserController.DEFAULT_PAGE_SIZE)).thenReturn(table);

        DataTable result = controller.loadTable("users");

        verify(reader).getRowCount("HR", "users");
        verify(reader).getColumnNames("HR", "users");
        verify(reader).loadTablePage("HR", "users", List.of(), "id", 0, DataBrowserController.DEFAULT_PAGE_SIZE);
        assertEquals("users", controller.getCurrentTableName());
        assertEquals(1, controller.getCurrentTable().getRowCount());
    }


    @Test
    void loadTable_removesPasswordColumns_test() throws SQLException {
        DataTable table = new DataTable(List.of("id", "username", "password"));
        table.addRow(new Object[]{1, "teodor", "secret123"});

        when(reader.getRowCount("HR", "users")).thenReturn(1L);
        when(reader.getColumnNames("HR", "users")).thenReturn(List.of("id", "username", "password"));
        when(reader.loadTablePage("HR", "users", List.of(), "id", 0, DataBrowserController.DEFAULT_PAGE_SIZE)).thenReturn(table);

        DataTable result = controller.loadTable("users");

        assertEquals(List.of("id", "username"), result.getColumnNames());
        assertArrayEquals(new Object[]{1, "teodor"}, result.getRows().get(0));
    }
    @Test
    void nextPage_loadsNextPage_test() throws SQLException {
        DataTable firstPage = new DataTable(List.of("id", "name"));
        firstPage.addRow(new Object[]{1, "Teo"});

        DataTable secondPage = new DataTable(List.of("id", "name"));
        secondPage.addRow(new Object[]{101, "John"});

        when(reader.getRowCount("HR", "users")).thenReturn(250L);
        when(reader.getColumnNames("HR", "users")).thenReturn(List.of("id", "name"));
        when(reader.loadTablePage("HR", "users", List.of(), "id", 0, 100)).thenReturn(firstPage);
        when(reader.loadTablePage("HR", "users", List.of(), "id", 100, 100)).thenReturn(secondPage);

        controller.loadTable("users");
        assertEquals(1, controller.getCurrentPageDisplay());

        DataTable result = controller.nextPage();

        // Verify the returned data
        assertEquals(List.of("id", "name"), result.getColumnNames());
        assertEquals(1, result.getRowCount());

        assertArrayEquals(new Object[]{101, "John"}, result.getRows().get(0));
        // Verify controller state
        assertEquals(2, controller.getCurrentPageDisplay());

        // Verify database interaction
        verify(reader).loadTablePage("HR", "users", List.of(), "id", 100, 100);
    }
    @Test
    void nextPage_onLastPage_throwsException() throws SQLException {
        DataTable table = new DataTable(List.of("id"));
        when(reader.getRowCount("HR", "users")).thenReturn(50L);
        when(reader.getColumnNames("HR", "users")).thenReturn(List.of("id"));
        when(reader.loadTablePage("HR", "users", List.of(), "id", 0, 100)).thenReturn(table);
        controller.loadTable("users");

        assertThrows(IllegalStateException.class, () -> controller.nextPage());

        verify(reader, times(1)).loadTablePage("HR", "users", List.of(), "id", 0, 100);
    }



    @Test
    void loadForeignKeys_test() throws SQLException {
        ForeignKeyInfo fk1 = new ForeignKeyInfo("FK_USERS_DEPARTMENT", "department_id", "HR", "departments", "id");
        ForeignKeyInfo fk2 = new ForeignKeyInfo("FK_USERS_ROLE", "role_id", "HR", "roles", "id");

        when(reader.getForeignKeys("HR", "users")).thenReturn(List.of(fk1, fk2));

        List<ForeignKeyInfo> result = controller.loadForeignKeys("users");

        assertEquals(2, result.size());
        assertEquals("FK_USERS_DEPARTMENT", result.get(0).getConstraintName());
        assertEquals("department_id", result.get(0).getFkColumn());
        assertEquals("HR", result.get(0).getReferencedOwner());
        assertEquals("departments", result.get(0).getReferencedTable());
        assertEquals("id", result.get(0).getReferencedColumn());
        assertEquals("FK_USERS_ROLE", result.get(1).getConstraintName());

        verify(reader).getForeignKeys("HR", "users");
    }
}