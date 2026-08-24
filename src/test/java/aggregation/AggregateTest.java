package aggregation;
import dataexploreapp.aggregation.AggregationFunction;
import dataexploreapp.aggregation.Aggregator;
import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.validation.SQLValidator;
import dataexploreapp.encryption.PasswordEncryptionService;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AggregateTest {
    private DataTable table;
    public AggregateTest() {
    }
    @BeforeEach
    void setUp() {
        table = new DataTable(List.of("id", "name", "date", "score"));
    }

    @Test
    public void empty_table_test() {
        DataTable empty = new DataTable(List.of("id", "name", "date", "score"));
        DataTable result_table=Aggregator.aggregate(empty,"id","name", AggregationFunction.SUM);
        assertEquals(0,result_table.getRows().size());
    }
    @Test
    public void columns_exist_test() throws IOException {
        assertDoesNotThrow(() -> Aggregator.aggregate(table,"id","name", AggregationFunction.COUNT));
    }
    @Test
    public void columns_dont_exist_test() throws IOException {
        //group column
        assertThrows(IllegalArgumentException.class,() -> Aggregator.aggregate(table,"kolona","name", AggregationFunction.SUM));
        //value column
        assertThrows(IllegalArgumentException.class,() -> Aggregator.aggregate(table,"id","kolona", AggregationFunction.SUM));
        //both dont exist
        assertThrows(IllegalArgumentException.class,() -> Aggregator.aggregate(table,"grupa","vrednost", AggregationFunction.SUM));
    }
    @Test
    public void null_cols_test() throws IOException {
        assertThrows(NullPointerException.class,() -> Aggregator.aggregate(table,null,"name", AggregationFunction.SUM));
        assertThrows(NullPointerException.class,() -> Aggregator.aggregate(table,"name",null, AggregationFunction.SUM));
        assertThrows(NullPointerException.class,() -> Aggregator.aggregate(table,null,null, AggregationFunction.SUM));

    }

    @Test void sum_test() throws IOException {
        table.addRow(new Object[]{1, "Teodor","24/08/2026",5000});
        table.addRow(new Object[]{2, "Mila","25/08/2026",15000});
        table.addRow(new Object[]{1, "Teodor","26/08/2026",3000});
        DataTable result_table=Aggregator.aggregate(table,"id","score",AggregationFunction.SUM);
        Object [] row= result_table.getRows().get(0);
        assertEquals(8000.0,row[1]);
        //negative values
        table.addRow(new Object[]{1, "Teodor","27/08/2026",-3000});
        DataTable result_table2=Aggregator.aggregate(table,"id","score",AggregationFunction.SUM);
        Object [] row2= result_table2.getRows().get(0);
        assertEquals(5000.0,row2[1]);
    }
    @Test void count_test() throws IOException {
        table.addRow(new Object[]{1, "Teodor","24/08/2026",5000});
        table.addRow(new Object[]{2, "Mila","25/08/2026",15000});
        table.addRow(new Object[]{1, "Teodor","26/08/2026",3000});
        DataTable result_table=Aggregator.aggregate(table,"id","score",AggregationFunction.COUNT);
        Object [] row= result_table.getRows().get(0);
        assertEquals(2.0,row[1]);
        //test za requiresValueColumn()
        DataTable result_table2=Aggregator.aggregate(table,"id",null,AggregationFunction.COUNT);
        Object [] points= result_table2.getRows().get(0);
        assertEquals(2.0,points[1]);
    }
    @Test void avg_test() throws IOException {
        table.addRow(new Object[]{1, "Teodor","24/08/2026",5000});
        table.addRow(new Object[]{2, "Mila","25/08/2026",15000});
        table.addRow(new Object[]{1, "Teodor","26/08/2026",3000});
        DataTable result_table=Aggregator.aggregate(table,"id","score",AggregationFunction.AVG);
        Object [] row= result_table.getRows().get(0);
        assertEquals(4000.0,row[1]);
    }

    @Test void max_test() throws IOException {
        table.addRow(new Object[]{1, "Teodor","24/08/2026",5000});
        table.addRow(new Object[]{2, "Mila","25/08/2026",15000});
        table.addRow(new Object[]{1, "Teodor","26/08/2026",3000});
        DataTable result_table=Aggregator.aggregate(table,"id","score",AggregationFunction.MAX);
        Object [] row= result_table.getRows().get(0);
        assertEquals(5000.0,row[1]);
    }
    @Test void min_test() throws IOException {
        table.addRow(new Object[]{1, "Teodor","24/08/2026",5000});
        table.addRow(new Object[]{2, "Mila","25/08/2026",15000});
        table.addRow(new Object[]{1, "Teodor","26/08/2026",3000});
        DataTable result_table=Aggregator.aggregate(table,"id","score",AggregationFunction.MIN);
        Object [] row= result_table.getRows().get(0);
        assertEquals(3000.0,row[1]);
    }






}
