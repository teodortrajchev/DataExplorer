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
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataQualityTest {
    List<Map<String,Object>> rows = new ArrayList<>();
    @BeforeEach
    public void setUp() {
        Map<String, Object> row1 = Map.of(
                "id", 1,
                "name", "John",
                "email", "john@test.com");

        Map<String, Object> row2 = Map.of(
                "id", 2,
                "name", "Smith",
                "email", "smith@test.com");
        Map<String, Object> row3 = Map.of(
                "id", 2,
                "name", "Smith",
                "email", "smith@test.com");
        Map<String, Object> row4 = Map.of(
                "id", 2,
                "name", "",
                "email", "t@test.com");
        Map<String, Object> row5 = Map.of(
                "id", 2,
                "name", "Mith",
                "email", "mithtest.com");
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);
    }
    //null ili prazno treba da vrati 100 score
    @Test
    void analyze_null_test() {
        DataQualityReport report = DataQualityAnalyzer.analyze(null);
        assertEquals(100.0, report.getScore());
        assertEquals(0, report.getDuplicateRows());
        assertEquals(0.0, report.getMissingPercentage());
        assertEquals(0, report.getInvalidEmails());
    }

    @Test
    void analyze_empty_test() {
        DataQualityReport report = DataQualityAnalyzer.analyze(List.of());
        assertEquals(100.0, report.getScore());
        assertEquals(0, report.getDuplicateRows());
        assertEquals(0.0, report.getMissingPercentage());
        assertEquals(0, report.getInvalidEmails());
    }
    @Test
    void analyze_values_test() {
        DataQualityReport report = DataQualityAnalyzer.analyze(rows);
        assertEquals(88.33333333333333, report.getScore());
        assertEquals(1, report.getDuplicateRows());
        assertEquals(6.666666666666667, report.getMissingPercentage());
        assertEquals(1, report.getInvalidEmails());
    }


    @ParameterizedTest
    @ValueSource(strings= {""," ","nesto@gmail","nestodrugo.com","@nesto.com","treto.com","chetvrto@.com"})
    void findInvalidEmails_false_test(String email){
        Map<String, Object> row = Map.of("email", email);
        DataQualityReport report = DataQualityAnalyzer.analyze(List.of(row));
        assertEquals(1, report.getInvalidEmails());
    }
    @Test
    void findInvalidEmails_true_test(){
        Map<String, Object> row = Map.of("email", "nesto@gmail.com");
        DataQualityReport report = DataQualityAnalyzer.analyze(List.of(row));
        assertEquals(0, report.getInvalidEmails());
    }



}
