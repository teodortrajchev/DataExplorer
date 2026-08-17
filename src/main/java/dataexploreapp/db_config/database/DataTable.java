package dataexploreapp.db_config.database;
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

     // true if every non-null value in the column parses as a number.decide whether sortBy compares numerically or as text.
    public boolean isNumericColumn(String columnName) {
        int colIndex = columnNames.indexOf(columnName);
        if (colIndex == -1) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }
        boolean sawValue = false;
        for (Object[] row : rows) {
            Object value = row[colIndex];
            if (value == null) continue;
            sawValue = true;
            if (toNullableDouble(value) == null) {
                return false;
            }
        }
        return sawValue; // an empty/all-null column isn't considered numeric
    }

    public DataTable sortBy(String columnName, boolean ascending) {
        int colIndex = columnNames.indexOf(columnName);
        if (colIndex == -1) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }

        boolean numeric = isNumericColumn(columnName);
        List<Object[]> sortedRows = new ArrayList<>(rows);

        Comparator<Object[]> comparator = numeric
                ? Comparator.comparing(
                (Object[] r) -> toNullableDouble(r[colIndex]),
                Comparator.nullsFirst(Comparator.naturalOrder()))
                : Comparator.comparing(
                (Object[] r) -> r[colIndex] == null ? "" : r[colIndex].toString(),
                String.CASE_INSENSITIVE_ORDER);

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

    private static Double toNullableDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}