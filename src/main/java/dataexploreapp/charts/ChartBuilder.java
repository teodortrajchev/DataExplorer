package dataexploreapp.charts;

import dataexploreapp.db_config.database.DataTable;
import javafx.scene.chart.*;

import java.util.List;


public class ChartBuilder {

    public enum ChartType {
        BAR,
        LINE,
        AREA,
        SCATTER,
        PIE,
        STACKED_BAR,
        STACKED_AREA
    }

    public static Chart build(DataTable table, ChartType type, String categoryColumn, List<String> valueColumns) {
        return switch (type) {
            case BAR -> buildBarChart(table, categoryColumn, valueColumns);
            case LINE -> buildLineChart(table, categoryColumn, valueColumns);
            case AREA -> buildAreaChart(table, categoryColumn, valueColumns);
            case SCATTER -> buildScatterChart(table, categoryColumn, valueColumns);
            case PIE -> buildPieChart(table, categoryColumn, valueColumns.get(0));
            case STACKED_BAR -> buildStackedBarChart(table, categoryColumn, valueColumns);
            case STACKED_AREA -> buildStackedAreaChart(table, categoryColumn, valueColumns);
        };
    }

    private static BarChart<String, Number> buildBarChart(DataTable table, String categoryColumn, List<String> valueColumns) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        BarChart<String, Number> chart =
                new BarChart<>(xAxis, yAxis);

        chart.setTitle("Bar Chart");

        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
        }

        chart.setLegendVisible(true);
        chart.setAnimated(false);
        return chart;
    }

    private static LineChart<String, Number> buildLineChart(DataTable table, String categoryColumn,  List<String> valueColumns) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        LineChart<String, Number> chart =
                new LineChart<>(xAxis, yAxis);

        chart.setTitle("Line Chart");

        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
        }

        chart.setLegendVisible(true);
        chart.setAnimated(false);
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
    private static ScatterChart<String, Number> buildScatterChart(
            DataTable table,
            String categoryColumn,
            List<String> valueColumns) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        ScatterChart<String, Number> chart =
                new ScatterChart<>(xAxis, yAxis);

        chart.setTitle("Scatter Chart");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
        }

        return chart;
    }
    private static AreaChart<String, Number> buildAreaChart(
            DataTable table,
            String categoryColumn,
            List<String> valueColumns) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        AreaChart<String, Number> chart =
                new AreaChart<>(xAxis, yAxis);

        chart.setTitle("Area Chart");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
        }

        return chart;
    }
    private static BarChart<String, Number> buildStackedBarChart(
            DataTable table,
            String categoryColumn,
            List<String> valueColumns) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        BarChart<String, Number> chart =
                new BarChart<>(xAxis, yAxis);

        chart.setTitle("Stacked Bar Chart");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        if (valueColumns.isEmpty()) {
            return chart;
        }

        /*
         * JavaFX does not have a native stacked bar chart.
         *
         * For now, create one series for each selected column.
         * The CSS can then be used to visually group them.
         */
        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
        }

        return chart;
    }
    private static StackedAreaChart<String, Number> buildStackedAreaChart(
            DataTable table,
            String categoryColumn,
            List<String> valueColumns) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel(categoryColumn);
        yAxis.setLabel("Value");

        StackedAreaChart<String, Number> chart =
                new StackedAreaChart<>(xAxis, yAxis);

        chart.setTitle("Stacked Area Chart");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        for (String valueColumn : valueColumns) {

            XYChart.Series<String, Number> series =
                    buildSeries(table, categoryColumn, valueColumn);

            series.setName(valueColumn);

            chart.getData().add(series);
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