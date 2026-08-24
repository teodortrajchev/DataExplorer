package db;
import dataexploreapp.db_config.database.DataTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataTableTest {
    DataTable table;

    @BeforeEach
    void setup() {
        table = new DataTable(List.of("id", "name"));
        table.addRow(new Object[]{1, "Teo"});
        table.addRow(new Object[]{2, "Teodor"});
        table.addRow(new Object[]{3, "Mikkel"});
        table.addRow(new Object[]{4, "Rubick"});
    }

    @Test
    void addRow_test() {
        assertEquals(4, table.getRows().size());
    }
    @Test
    void addRow_invalid_columns_test() {
        assertThrows(IllegalArgumentException.class, () -> table.addRow(new Object[]{1}));
        assertThrows(IllegalArgumentException.class, () -> table.addRow(new Object[]{5,"Teo",3}));
        assertThrows(IllegalArgumentException.class, () -> table.addRow(new Object[]{}));
    }

    @Test
    void addRow_null_test(){
        assertThrows(NullPointerException.class, () -> table.addRow(null));
    }
    @Test
    void empty_table_test() {
        DataTable table = new DataTable(List.of());
        assertEquals(0, table.getRows().size());
    }
    @Test
    void column_number_test() {
        assertEquals(2,table.getColumnCount());
    }

    @Test
    void title_test() {
        table.setTitle("Users");
        assertEquals("Users", table.getTitle());
    }
//    FILTER
    @Test
    void filter_sameTitle_test() {
        table.setTitle("Users");
        DataTable filtered = table.filterContains("name", "Teo");
        assertEquals("Users", filtered.getTitle());
    }
    @Test
    void filterContains_test() {
        DataTable result = table.filterContains("name", "Teo");
        assertEquals(2, result.getRowCount());
        assertEquals(1, result.getRows().get(0)[0]);
        assertEquals("Teo", result.getRows().get(0)[1]);
        assertEquals(2, result.getRows().get(1)[0]);
        assertEquals("Teodor", result.getRows().get(1)[1]);
    }
    @Test
    void filter_caseInsensitive_test() {
        DataTable result = table.filterContains("name", "TEO");
        assertEquals(2, result.getRowCount());
    }

    @Test
    void filterContains_noMatches_test() {
        DataTable result = table.filterContains("name", "Marko");
        assertEquals(0, result.getRowCount());
    }
    @Test
    void filterContains_invalidColumn_test() {
        assertThrows(IllegalArgumentException.class, () -> table.filterContains("age", "21"));
    }
    @Test
    void filterContains_nullValue_test() {
        table.addRow(new Object[]{5, null});
        DataTable result = table.filterContains("name", "teo");
        assertEquals(2, result.getRowCount());
    }
    @Test
    void filterContains_empty_test() {
        table.addRow(new Object[]{5, null});
        DataTable result = table.filterContains("name", "");
        assertEquals(5, result.getRowCount());
    }


//    WithoutColumnsContaining
    @Test
    void withoutColumnsContaining_shouldBeCaseInsensitive() {
        DataTable testTable = new DataTable(List.of("id", "name", "password"));
        DataTable result = testTable.withoutColumnsContaining("password");
        assertEquals(List.of("id", "name"), result.getColumnNames());
    }
//    isNumericColumn
    @Test
    void column_isNumeric_test() {
        table.addRow(new Object[]{1,"Teo"});
        assertTrue(table.isNumericColumn("id"));
        assertFalse(table.isNumericColumn("name"));
    }
    @Test
    void isNumericColumn_decimal_negative_test() {
        DataTable test = new DataTable(List.of("val"));
        test.addRow(new Object[]{"100"});
        test.addRow(new Object[]{"25.5"});
        test.addRow(new Object[]{"-10"});
        assertTrue(test.isNumericColumn("val"));
    }
    @Test
    void isNumericColumn_mixed_test() {
        DataTable test = new DataTable(List.of("value"));

        test.addRow(new Object[]{"100"});
        test.addRow(new Object[]{"hello"});
        test.addRow(new Object[]{"50"});
        assertFalse(test.isNumericColumn("value"));
    }
    @Test
    void isNumericColumn_null_test() {
        DataTable test = new DataTable(List.of("value"));
        test.addRow(new Object[]{null});
        test.addRow(new Object[]{100});
        test.addRow(new Object[]{200});
        assertTrue(test.isNumericColumn("value"));
    }
    @Test
    void isNumericColumn_empty_test() {
        DataTable empty = new DataTable(List.of("value"));
        assertFalse(empty.isNumericColumn("value"));
    }
    @Test
    void isNumericColumn_invalid_test() {
        assertThrows(IllegalArgumentException.class, () -> table.isNumericColumn("age"));
    }

//    sortBy
    @Test
    void sortBy_numeric_asc_test() {
        DataTable test = new DataTable(List.of("id", "name"));
        test.addRow(new Object[]{30, "A"});
        test.addRow(new Object[]{10, "B"});
        test.addRow(new Object[]{20, "C"});

        DataTable result = test.sortBy("id", true);

        assertEquals(10, result.getRows().get(0)[0]);
        assertEquals(20, result.getRows().get(1)[0]);
        assertEquals(30, result.getRows().get(2)[0]);
    }
    @Test
    void sortBy_numeric_desc_test() {
        DataTable test = new DataTable(List.of("id", "name"));
        test.addRow(new Object[]{30, "A"});
        test.addRow(new Object[]{10, "B"});
        test.addRow(new Object[]{20, "C"});

        DataTable result = test.sortBy("id", false);

        assertEquals(30, result.getRows().get(0)[0]);
        assertEquals(20, result.getRows().get(1)[0]);
        assertEquals(10, result.getRows().get(2)[0]);
    }
    @Test
    void sortBy_string_asc_test() {
        DataTable result = table.sortBy("name", true);
        assertEquals("Mikkel", result.getRows().get(0)[1]);
        assertEquals("Rubick", result.getRows().get(1)[1]);
        assertEquals("Teo", result.getRows().get(2)[1]);
        assertEquals("Teodor", result.getRows().get(3)[1]);
    }
    @Test
    void sortBy_negative_num_test() {
        DataTable test = new DataTable(List.of("score"));
        test.addRow(new Object[]{10});
        test.addRow(new Object[]{-20});
        test.addRow(new Object[]{5});
        test.addRow(new Object[]{-1});

        DataTable result = test.sortBy("score", true);

        assertEquals(-20, result.getRows().get(0)[0]);
        assertEquals(-1, result.getRows().get(1)[0]);
        assertEquals(5, result.getRows().get(2)[0]);
        assertEquals(10, result.getRows().get(3)[0]);
    }

    @Test
    void sortBy_null_num_test() {
        DataTable test = new DataTable(List.of("score"));
        test.addRow(new Object[]{10});
        test.addRow(new Object[]{null});
        test.addRow(new Object[]{5});

        DataTable result = test.sortBy("score", true);

        assertNull(result.getRows().get(0)[0]);
        assertEquals(5, result.getRows().get(1)[0]);
        assertEquals(10, result.getRows().get(2)[0]);
    }

    @Test
    void sortBy_invalid_test() {
        assertThrows(IllegalArgumentException.class, () -> table.sortBy("age", true));
    }
}
