package dataexploreapp.filtering;

import dataexploreapp.db_config.database.DataTable;

import java.util.List;


public final class DataFilterEngine {

    private DataFilterEngine() {
    }

    public static DataTable apply(DataTable table, List<FilterCondition> conditions, FilterCombinator combinator) {
        if (conditions.isEmpty()) {
            return table;
        }

        List<Integer> columnIndices = conditions.stream()
                .map(c -> {
                    int idx = table.getColumnNames().indexOf(c.getColumn());
                    if (idx == -1) {
                        throw new IllegalArgumentException("Unknown column: " + c.getColumn());
                    }
                    return idx;
                })
                .toList();

        DataTable filtered = new DataTable(table.getColumnNames());
        filtered.setTitle(table.getTitle());

        for (Object[] row : table.getRows()) {
            if (evaluateRow(row, conditions, columnIndices, combinator)) {
                filtered.addRow(row);
            }
        }
        return filtered;
    }

    private static boolean evaluateRow(Object[] row, List<FilterCondition> conditions, List<Integer> columnIndices, FilterCombinator combinator) {
        if (combinator == FilterCombinator.AND) {
            for (int i = 0; i < conditions.size(); i++) {
                if (!matches(row[columnIndices.get(i)], conditions.get(i))) return false;
            }
            return true;
        } else {
            for (int i = 0; i < conditions.size(); i++) {
                if (matches(row[columnIndices.get(i)], conditions.get(i))) return true;
            }
            return false;
        }
    }

    private static boolean matches(Object cellValue, FilterCondition condition) {
        FilterOperator op = condition.getOperator();

        if (op == FilterOperator.CONTAINS) {
            String text = cellValue == null ? "" : cellValue.toString().toLowerCase();
            return text.contains(condition.getValue().toLowerCase());
        }

        if (op == FilterOperator.EQUALS) {
            Double cellNum = toDoubleOrNull(cellValue);
            Double filterNum = toDoubleOrNull(condition.getValue());
            if (cellNum != null && filterNum != null) {
                return cellNum.equals(filterNum);
            }
            String cellText = cellValue == null ? "" : cellValue.toString();
            return cellText.equalsIgnoreCase(condition.getValue());
        }

        // Everything below is a numeric comparison — a non-numeric cell never matches.
        Double cellNum = toDoubleOrNull(cellValue);
        if (cellNum == null) return false;

        return switch (op) {
            case GREATER_THAN -> cellNum > parseRequired(condition.getValue());
            case LESS_THAN -> cellNum < parseRequired(condition.getValue());
            case GREATER_OR_EQUAL -> cellNum >= parseRequired(condition.getValue());
            case LESS_OR_EQUAL -> cellNum <= parseRequired(condition.getValue());
            case BETWEEN -> {
                double lo = parseRequired(condition.getValue());
                double hi = parseRequired(condition.getSecondValue());
                if (lo > hi) { double tmp = lo; lo = hi; hi = tmp; } // tolerate swapped bounds
                yield cellNum >= lo && cellNum <= hi;
            }
            default -> false;
        };
    }

    private static Double toDoubleOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseRequired(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a number, got: " + value);
        }
    }
}