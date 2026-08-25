package db;
import dataexploreapp.aggregation.AggregationFunction;
import dataexploreapp.aggregation.Aggregator;
import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.dataquality.DataQualityAnalyzer;
import dataexploreapp.db_config.dataquality.DataQualityReport;
import dataexploreapp.db_config.validation.SQLValidator;
import dataexploreapp.encryption.PasswordEncryptionService;
import dataexploreapp.filtering.DataFilterEngine;
import dataexploreapp.filtering.FilterCombinator;
import dataexploreapp.filtering.FilterCondition;
import dataexploreapp.filtering.FilterOperator;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataFilterTest {

    DataTable table;
    @BeforeEach
    void setup() {
        table = new DataTable(List.of("id", "name"));
        table.addRow(new Object[]{1, "Teo"});
        table.addRow(new Object[]{2, "Teodor"});
        table.addRow(new Object[]{3, "Mikkel"});
        table.addRow(new Object[]{4, "Rubick"});
    }
    // Numeric
    static Stream<Arguments> numericFilters() {
        return Stream.of(
                Arguments.of(FilterOperator.GREATER_THAN, "2", 2),
                Arguments.of(FilterOperator.LESS_THAN, "3", 2),
                Arguments.of(FilterOperator.GREATER_OR_EQUAL, "3", 2),
                Arguments.of(FilterOperator.LESS_OR_EQUAL, "2", 2),
                Arguments.of(FilterOperator.EQUALS, "2", 1)
        );
    }


    @ParameterizedTest
    @MethodSource("numericFilters")
    void numericFilter_test(FilterOperator operator, String value, int expectedRows) {
        FilterCondition condition = new FilterCondition("id", operator, value);
        DataTable result = DataFilterEngine.apply(table, List.of(condition), FilterCombinator.AND);
        assertEquals(expectedRows, result.getRows().size());
    }


    // Text
    static Stream<Arguments> textFilters() {
        return Stream.of(
                Arguments.of(FilterOperator.EQUALS, "TEO", 1),
                Arguments.of(FilterOperator.EQUALS, "teodor", 1),
                Arguments.of(FilterOperator.CONTAINS, "teo", 2),
                Arguments.of(FilterOperator.CONTAINS, "MIK", 1),
                Arguments.of(FilterOperator.CONTAINS, "xyz", 0)
        );
    }

    @ParameterizedTest
    @MethodSource("textFilters")
    void textFilter_test(FilterOperator operator, String value, int expectedRows) {
        FilterCondition condition = new FilterCondition("name", operator, value);

        DataTable result = DataFilterEngine.apply(table, List.of(condition), FilterCombinator.AND);
        assertEquals(expectedRows, result.getRows().size());
    }


    // Between + zameneti granici
    static Stream<Arguments> betweenFilters() {
        return Stream.of(
                Arguments.of("2", "3", 2),
                Arguments.of("1", "4", 4),
                Arguments.of("2", "2", 1),
                Arguments.of("3", "2", 2),
                Arguments.of("10", "20", 0)
        );
    }
    @ParameterizedTest
    @MethodSource("betweenFilters")
    void betweenFilter_test(String lower, String upper, int expectedRows) {
        FilterCondition condition = new FilterCondition("id", FilterOperator.BETWEEN, lower, upper);

        DataTable result = DataFilterEngine.apply(table, List.of(condition), FilterCombinator.AND);
        assertEquals(expectedRows, result.getRows().size());
    }

    //AND so povekje uslovi
    @Test
    void andFilter_multiple_cond_test() {
        FilterCondition idCondition = new FilterCondition("id", FilterOperator.GREATER_THAN, "1");
        FilterCondition nameCondition = new FilterCondition("name", FilterOperator.CONTAINS, "eo");
        DataTable result = DataFilterEngine.apply(table, List.of(idCondition, nameCondition), FilterCombinator.AND);

        assertEquals(1, result.getRows().size());
        assertEquals(2, result.getRows().get(0)[0]);
    }

    //OR barem eden uslov ispolnet
    @Test
    void orFilter_test() {
        FilterCondition first = new FilterCondition("id",FilterOperator.EQUALS,"1");
        FilterCondition second = new FilterCondition("id",FilterOperator.EQUALS,"4");

        DataTable result = DataFilterEngine.apply(table, List.of(first, second), FilterCombinator.OR);
        assertEquals(2, result.getRows().size());
    }

    //Nepostoecka kolona
    @Test
    void invalid_column_test() {
        FilterCondition condition = new FilterCondition("age",FilterOperator.EQUALS,"20");
        assertThrows(IllegalArgumentException.class,() -> DataFilterEngine.apply(table, List.of(condition), FilterCombinator.AND));
    }


    //String kaj int pole
    @Test
    void invalid_num_test() {
        FilterCondition condition = new FilterCondition("id",FilterOperator.GREATER_THAN,"abc");

        assertThrows(IllegalArgumentException.class,() -> DataFilterEngine.apply(table, List.of(condition), FilterCombinator.AND));
    }

}
