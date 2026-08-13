package dataexportapp.charts;

import dataexportapp.db_config.database.DataTable;
import javafx.scene.chart.*;


public class ChartBuilder {

    public enum ChartType { BAR, LINE, PIE }

    public static Chart build(DataTable table, ChartType type, String categoryColumn, String valueColumn) {
        return switch (type) {
            case BAR -> buildBarChart(table, categoryColumn, valueColumn);
            case LINE -> buildLineChart(table, categoryColumn, valueColumn);
            case PIE -> buildPieChart(table, categoryColumn, valueColumn);
        };
    }

    private static BarChart<String, Number> buildBarChart(DataTable table, String categoryColumn, String valueColumn) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setTitle(table.getTitle());
        chart.getData().add(buildSeries(table, categoryColumn, valueColumn));
        return chart;
    }

    private static LineChart<String, Number> buildLineChart(DataTable table, String categoryColumn, String valueColumn) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        chart.setTitle(table.getTitle());
        chart.getData().add(buildSeries(table, categoryColumn, valueColumn));
        return chart;
    }

    private static PieChart buildPieChart(DataTable table, String categoryColumn, String valueColumn) {
        PieChart chart = new PieChart();
        chart.setTitle(table.getTitle());

        int catIdx = table.getColumnNames().indexOf(categoryColumn);
        int valIdx = table.getColumnNames().indexOf(valueColumn);

        for (Object[] row : table.getRows()) {
            chart.getData().add(new PieChart.Data(String.valueOf(row[catIdx]), toDouble(row[valIdx])));
        }
        return chart;
    }

    private static XYChart.Series<String, Number> buildSeries(DataTable table, String categoryColumn, String valueColumn) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int catIdx = table.getColumnNames().indexOf(categoryColumn);
        int valIdx = table.getColumnNames().indexOf(valueColumn);

        for (Object[] row : table.getRows()) {
            series.getData().add(new XYChart.Data<>(String.valueOf(row[catIdx]), toDouble(row[valIdx])));
        }
        return series;
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