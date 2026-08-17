package dataexploreapp.controllers;

import dataexploreapp.aggregation.AggregationFunction;
import dataexploreapp.aggregation.Aggregator;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.database.ForeignKeyInfo;
import dataexploreapp.db_config.database.ProcedureParameter;
import dataexploreapp.db_config.validation.SQLValidator;
import dataexploreapp.history.ExportRecord;
import dataexploreapp.importfiles.DataImportService;
import dataexploreapp.filtering.DataFilterEngine;
import dataexploreapp.filtering.FilterCombinator;
import dataexploreapp.filtering.FilterCondition;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataBrowserController {

    private final DataBaseReader reader;
    private final String schema;

    private DataTable originalTable;
    private DataTable currentTable;
    private boolean currentIsProcedure;
    private String currentImportedFilePath;
    private String currentTableName;
    private List<ForeignKeyInfo> currentForeignKeys = new ArrayList<>();


    //pagination
    public static final int DEFAULT_PAGE_SIZE = 100;

    private int pageSize = DEFAULT_PAGE_SIZE;
    private int currentPage = 0; // 0-based internally
    private long totalRowCount = 0;
    private String pageOrderByColumn;
    private boolean fullTableLoaded = false;

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

    public DataTable loadTable(String tableName) throws SQLException {
        currentTableName = tableName;
        currentPage = 0;
        fullTableLoaded = false;
        totalRowCount = reader.getRowCount(schema, tableName);

        List<String> columns = reader.getColumnNames(schema, tableName);
        pageOrderByColumn = columns.isEmpty() ? null : columns.get(0);

        DataTable raw = (pageOrderByColumn == null)
                ? reader.loadTableWithForeignKeys(schema, tableName, List.of())
                : reader.loadTablePage(schema, tableName, List.of(), pageOrderByColumn, 0, pageSize);

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

    public DataTable applyFilters(List<FilterCondition> conditions, FilterCombinator combinator) {
        requireLoadedTable();
        currentTable = DataFilterEngine.apply(originalTable, conditions, combinator);
        return currentTable;
    }

    public DataTable resetFilter() {
        requireLoadedTable();
        currentTable = originalTable;
        return currentTable;
    }

    public boolean isNumericColumn(String columnName) {
        return originalTable != null && originalTable.isNumericColumn(columnName);
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
            FilterCondition legacyCondition = new FilterCondition(
                    record.getFilterColumn(), dataexploreapp.filtering.FilterOperator.CONTAINS, record.getFilterValue());
            result = applyFilters(List.of(legacyCondition), FilterCombinator.AND);
        }
        if (hasSort) {
            result = applySort(record.getSortColumn(), Boolean.TRUE.equals(record.getSortAscending()));
        }

        return new ReapplyOutcome(hasFilter, hasSort,
                record.getFilterColumn(), record.getFilterValue(),
                record.getSortColumn(), record.getSortAscending(),
                result);
    }
    public DataTable applyAggregation(String groupColumn, String valueColumn, AggregationFunction function) {
        requireLoadedTable();
        currentTable = Aggregator.aggregate(currentTable, groupColumn, valueColumn, function);
        return currentTable;
    }
    public DataTable buildAggregatedChartData(String groupColumn, String valueColumn, AggregationFunction function) {
        requireLoadedTable();
        return Aggregator.aggregate(currentTable, groupColumn, valueColumn, function);
    }


    public DataTable runRawQuery(String sql) throws SQLException {
        SQLValidator.validate(sql);
        currentTableName = null;
        currentForeignKeys = List.of();
        DataTable raw = reader.runQuery(sql);
        if (raw.getTitle() == null || raw.getTitle().isBlank()) {
            raw.setTitle("Query result");
        }
        return adoptFreshTable(raw, false, null);
    }

    public DataTable nextPage() throws SQLException {
        requirePaginatedTable();
        if ((long) (currentPage + 1) * pageSize >= totalRowCount) {
            throw new IllegalStateException("Already on the last page.");
        }
        currentPage++;
        return loadCurrentPage();
    }

    public DataTable previousPage() throws SQLException {
        requirePaginatedTable();
        if (currentPage == 0) {
            throw new IllegalStateException("Already on the first page.");
        }
        currentPage--;
        return loadCurrentPage();
    }

    public DataTable setPageSize(int newPageSize) throws SQLException {
        requirePaginatedTable();
        this.pageSize = newPageSize;
        this.currentPage = 0;
        return loadCurrentPage();
    }

    public DataTable loadFullTable() throws SQLException {
        requirePaginatedTable();
        DataTable raw = reader.loadTableWithForeignKeys(schema, currentTableName, List.of());
        fullTableLoaded = true;
        return adoptFreshTable(raw, false, null);
    }

    private DataTable loadCurrentPage() throws SQLException {
        DataTable raw = reader.loadTablePage(schema, currentTableName, List.of(), pageOrderByColumn, currentPage * pageSize, pageSize);
        return adoptFreshTable(raw, false, null);
    }

    private void requirePaginatedTable() {
        if (currentTableName == null) {
            throw new IllegalStateException("No table currently loaded.");
        }
        if (fullTableLoaded) {
            throw new IllegalStateException("Full table is loaded — pagination isn't active.");
        }
    }
    public DataBaseReader getReader() {
        return reader;
    }

    // --- Pagination state for the UI to read -----------------------------

    public boolean isPaginated() { return currentTableName != null && !fullTableLoaded; }
    public boolean isFullTableLoaded() { return fullTableLoaded; }
    public int getCurrentPageDisplay() { return currentPage + 1; } // 1-based for UI
    public int getTotalPages() { return totalRowCount == 0 ? 1 : (int) Math.ceil((double) totalRowCount / pageSize); }
    public long getTotalRowCount() { return totalRowCount; }
    public int getPageSize() { return pageSize; }
    // --- State accessors -------------------------------------------------

    public DataTable getCurrentTable() { return currentTable; }
    public DataTable getOriginalTable() { return originalTable; }
    public boolean isCurrentProcedure() { return currentIsProcedure; }
    public String getCurrentImportedFilePath() { return currentImportedFilePath; }
    public String getCurrentTableName() { return currentTableName; }
    public List<ForeignKeyInfo> getCurrentForeignKeys() { return currentForeignKeys; }
    public String getSchema() { return schema; }
}