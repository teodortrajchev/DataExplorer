package DataExportApp.History;

public class ExportRecord {

    private String timestamp;
    private String tableName;
    private boolean isProcedure;
    private String format;
    private String filterColumn;
    private String filterValue;
    private String sortColumn;
    private Boolean sortAscending;
    private String outputPath;
    private String filePath;
    private int rowCount;

    public ExportRecord() {
    }

    public ExportRecord(String timestamp, String tableName, boolean isProcedure, String filePath, String format,
                        String filterColumn, String filterValue,
                        String sortColumn, Boolean sortAscending,
                        String outputPath, int rowCount) {
        this.timestamp = timestamp;
        this.tableName = tableName;
        this.isProcedure = isProcedure;
        this.filePath = filePath;
        this.format = format;
        this.filterColumn = filterColumn;
        this.filterValue = filterValue;
        this.sortColumn = sortColumn;
        this.sortAscending = sortAscending;
        this.outputPath = outputPath;
        this.rowCount = rowCount;
    }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public boolean isProcedure() { return isProcedure; }
    public void setProcedure(boolean procedure) { isProcedure = procedure; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getFilterColumn() { return filterColumn; }
    public void setFilterColumn(String filterColumn) { this.filterColumn = filterColumn; }

    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }

    public String getSortColumn() { return sortColumn; }
    public void setSortColumn(String sortColumn) { this.sortColumn = sortColumn; }

    public Boolean getSortAscending() { return sortAscending; }
    public void setSortAscending(Boolean sortAscending) { this.sortAscending = sortAscending; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
}