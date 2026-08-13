package dataexportapp.controllers;


import dataexportapp.db_config.database.DataBaseReader;
import dataexportapp.db_config.database.DataTable;
import dataexportapp.db_config.database.ForeignKeyInfo;
import dataexportapp.db_config.database.ProcedureParameter;
import dataexportapp.history.ExportRecord;
import dataexportapp.importfiles.DataImportService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Owns all data-loading, filtering, sorting, and history-reapply logic for
 * the data browser screen. Deliberately has no JavaFX imports — it can be
 * constructed and tested with a mocked DataBaseReader, with no Stage/Scene
 * required. DataBrowserPage wraps every call here in a javafx.concurrent.Task
 * and is responsible for all UI construction, threading, and rendering.
 */
public class DataBrowserController {

    private final DataBaseReader reader;
    private final String schema;

    private DataTable originalTable;
    private DataTable currentTable;
    private boolean currentIsProcedure;
    private String currentImportedFilePath;
    private String currentTableName;
    private List<ForeignKeyInfo> currentForeignKeys = new ArrayList<>();

    private static final String SENSITIVE_COLUMN_KEYWORD = "password";

    public DataBrowserController(DataBaseReader reader, String schema) {
        this.reader = reader;
        this.schema = schema;
    }

    // --- Listing -----------------------------------------------------------

    public List<String> listTables() throws SQLException {
        return reader.listTables(schema);
    }

    public List<String> listProcedures() throws SQLException {
        return reader.listProcedures(schema);
    }

    // --- Loading a table -----------------------------------------------------

    /** Fresh load of a table with no joined foreign keys. */
    public DataTable loadTable(String tableName) throws SQLException {
        currentTableName = tableName;
        DataTable raw = reader.loadTableWithForeignKeys(schema, tableName, List.of());
        return adoptFreshTable(raw, false, null);
    }

    /** Reloads the currently selected table with a different set of joined foreign keys. */
    public DataTable reloadTableWithForeignKeys(List<ForeignKeyInfo> enabledForeignKeys) throws SQLException {
        if (currentTableName == null) {
            throw new IllegalStateException("No table currently loaded — call loadTable first.");
        }
        DataTable raw = reader.loadTableWithForeignKeys(schema, currentTableName, enabledForeignKeys);
        originalTable = raw.withoutColumnsContaining(SENSITIVE_COLUMN_KEYWORD);
        currentTable = originalTable;
        return currentTable;
    }

    public List<ForeignKeyInfo> loadForeignKeys(String tableName) throws SQLException {
        currentForeignKeys = reader.getForeignKeys(schema, tableName);
        return currentForeignKeys;
    }

    // --- Loading a procedure -------------------------------------------------

    public List<ProcedureParameter> getInputParameters(String procedureName) throws SQLException {
        return reader.getProcedureParameters(schema, procedureName).stream()
                .filter(ProcedureParameter::isInput)
                .toList();
    }

    public DataTable loadProcedure(String procedureName, Map<String, String> paramValues) throws SQLException {
        currentTableName = null;
        DataTable raw = reader.loadProcedure(schema, procedureName, paramValues);
        return adoptFreshTable(raw, true, null);
    }

    // --- Loading an imported file ---------------------------------------------

    public DataTable loadFile(File file) throws IOException {
        currentTableName = null;
        DataTable raw = DataImportService.importFile(file);
        return adoptFreshTable(raw, false, file.getAbsolutePath());
    }

    private DataTable adoptFreshTable(DataTable raw, boolean isProcedure, String importedFilePath) {
        originalTable = raw.withoutColumnsContaining(SENSITIVE_COLUMN_KEYWORD);
        currentTable = originalTable;
        currentIsProcedure = isProcedure;
        currentImportedFilePath = importedFilePath;
        return currentTable;
    }

    // --- Filter / sort (operate on whatever is currently loaded) -------------

    public DataTable applyFilter(String column, String value) {
        requireLoadedTable();
        currentTable = currentTable.filterContains(column, value);
        return currentTable;
    }

    public DataTable resetFilter() {
        requireLoadedTable();
        currentTable = originalTable;
        return currentTable;
    }

    public DataTable applySort(String column, boolean ascending) {
        requireLoadedTable();
        currentTable = currentTable.sortBy(column, ascending);
        return currentTable;
    }

    private void requireLoadedTable() {
        if (originalTable == null) {
            throw new IllegalStateException("No table currently loaded.");
        }
    }

    // --- History reapply -------------------------------------------------

    /**
     * Describes what reapplying a history record actually did, so the caller
     * (the Page) can update its filter/sort controls and status message to
     * match, without needing to know the reapply logic itself.
     */
    public record ReapplyOutcome(
            boolean hasFilter, boolean hasSort,
            String filterColumn, String filterValue,
            String sortColumn, Boolean sortAscending,
            DataTable resultTable
    ) {}

    /**
     * Reapplies a history record's filter/sort onto whatever table was just
     * freshly loaded (call this immediately after loadTable/loadProcedure/
     * loadFile, before any other filter/sort interaction).
     */
    public ReapplyOutcome reapplyRecord(ExportRecord record) {
        boolean hasFilter = record.getFilterColumn() != null
                && record.getFilterValue() != null
                && !record.getFilterValue().isBlank();
        boolean hasSort = record.getSortColumn() != null;

        DataTable result = currentTable;
        if (hasFilter) {
            result = applyFilter(record.getFilterColumn(), record.getFilterValue());
        }
        if (hasSort) {
            result = applySort(record.getSortColumn(), Boolean.TRUE.equals(record.getSortAscending()));
        }

        return new ReapplyOutcome(hasFilter, hasSort,
                record.getFilterColumn(), record.getFilterValue(),
                record.getSortColumn(), record.getSortAscending(),
                result);
    }

    // --- State accessors -------------------------------------------------

    public DataTable getCurrentTable() { return currentTable; }
    public DataTable getOriginalTable() { return originalTable; }
    public boolean isCurrentProcedure() { return currentIsProcedure; }
    public String getCurrentImportedFilePath() { return currentImportedFilePath; }
    public String getCurrentTableName() { return currentTableName; }
    public List<ForeignKeyInfo> getCurrentForeignKeys() { return currentForeignKeys; }
    public String getSchema() { return schema; }
}