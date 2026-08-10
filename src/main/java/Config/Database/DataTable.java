package Config.Database;
import java.util.ArrayList;
import java.util.List;


public class DataTable {

    private final List<String> columnNames;
    private final List<Object[]> rows = new ArrayList<>();
    private String title = "Exported Data";

    public DataTable(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void addRow(Object[] row) {
        if (row.length != columnNames.size()) {
            throw new IllegalArgumentException(
                    "Row has " + row.length + " values but table has " + columnNames.size() + " columns");
        }
        rows.add(row);
    }

    public List<String> getColumnNames() { return columnNames; }
    public List<Object[]> getRows() { return rows; }
    public int getColumnCount() { return columnNames.size(); }
    public int getRowCount() { return rows.size(); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
