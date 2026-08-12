package Config.Database;
import java.util.ArrayList;
import java.util.Comparator;
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

    public DataTable filterContains(String columnName, String searchText) {
        int colIndex = columnNames.indexOf(columnName);
        if (colIndex == -1) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }

        DataTable filtered = new DataTable(columnNames);
        filtered.setTitle(title);
        String needle = searchText.toLowerCase();

        for (Object[] row : rows) {
            Object value = row[colIndex];
            String text = value == null ? "" : value.toString().toLowerCase();
            if (text.contains(needle)) {
                filtered.addRow(row);
            }
        }
        return filtered;
    }
    public DataTable withoutColumnsContaining(String keyword) {
        String needle = keyword.toLowerCase();
        List<String> keptColumns = new ArrayList<>();
        List<Integer> keptIndices = new ArrayList<>();

        for (int i = 0; i < columnNames.size(); i++) {
            if (!columnNames.get(i).toLowerCase().contains(needle)) {
                keptColumns.add(columnNames.get(i));
                keptIndices.add(i);
            }
        }

        DataTable filtered = new DataTable(keptColumns);
        filtered.setTitle(title);

        for (Object[] row : rows) {
            Object[] newRow = new Object[keptIndices.size()];
            for (int i = 0; i < keptIndices.size(); i++) {
                newRow[i] = row[keptIndices.get(i)];
            }
            filtered.addRow(newRow);
        }

        return filtered;
    }
    public DataTable sortBy(String columnName, boolean ascending) {
        int colIndex = columnNames.indexOf(columnName);
        if (colIndex == -1) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }

        List<Object[]> sortedRows = new ArrayList<>(rows);
        Comparator<Object[]> comparator = Comparator.comparing(
                r -> r[colIndex] == null ? "" : r[colIndex].toString(),
                String.CASE_INSENSITIVE_ORDER
        );
        if (!ascending) {
            comparator = comparator.reversed();
        }
        sortedRows.sort(comparator);

        DataTable sorted = new DataTable(columnNames);
        sorted.setTitle(title);
        for (Object[] row : sortedRows) {
            sorted.addRow(row);
        }
        return sorted;
    }
}