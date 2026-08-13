package dataexploreapp.aggregation;


import dataexploreapp.db_config.database.DataTable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class Aggregator {

    private Aggregator() {
    }


    public static DataTable aggregate(DataTable table, String groupColumn, String valueColumn, AggregationFunction function) {
        int groupIdx = table.getColumnNames().indexOf(groupColumn);
        if (groupIdx == -1) {
            throw new IllegalArgumentException("Unknown group column: " + groupColumn);
        }
        int valueIdx = -1;
        if (function.requiresValueColumn()) {
            valueIdx = table.getColumnNames().indexOf(valueColumn);
            if (valueIdx == -1) {
                throw new IllegalArgumentException("Unknown value column: " + valueColumn);
            }
        }

        Map<String, List<Double>> groups = new LinkedHashMap<>();
        for (Object[] row : table.getRows()) {
            String key = row[groupIdx] == null ? "(null)" : row[groupIdx].toString();
            double value = valueIdx == -1 ? 0 : toDouble(row[valueIdx]);
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
        }

        DataTable result = new DataTable(List.of(groupColumn, function.describe(valueColumn)));
        result.setTitle(table.getTitle() + " — " + function.describe(valueColumn) + " by " + groupColumn);

        for (Map.Entry<String, List<Double>> entry : groups.entrySet()) {
            double aggregated = switch (function) {
                case SUM -> entry.getValue().stream().mapToDouble(Double::doubleValue).sum();
                case AVG -> entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                case COUNT -> entry.getValue().size();
                case MIN -> entry.getValue().stream().mapToDouble(Double::doubleValue).min().orElse(0);
                case MAX -> entry.getValue().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            };
            result.addRow(new Object[]{entry.getKey(), aggregated});
        }

        return result;
    }

    private static double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}