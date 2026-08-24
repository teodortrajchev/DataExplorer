import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.validation.SQLValidator;
import dataexploreapp.encryption.PasswordEncryptionService;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FirstTest{

    private SQLValidator validator;
    public FirstTest(){
        validator = new SQLValidator();
    }

    private static List<String> to_validate(){
        List<String> lista=new ArrayList<>();
        lista.add("DELETE * FROM app_users");
        lista.add("SELECT * FROM app_users");
        return lista;
    }

//    @ParameterizedTest
//    @MethodSource("to_validate")
//    public void testFirst() throws Exception {
//        assertThrows(IllegalArgumentException.class,()->SQLValidator.validate(to_validate().get(1)));
//        assertDoesNotThrow(()->validator.validate(to_validate().get(1)));
//    }
    @Test
    public void testSqlValidator() throws Exception {
        assertThrows(IllegalArgumentException.class,()->validator.validate(to_validate().get(0)));
        assertDoesNotThrow(()->validator.validate(to_validate().get(1)));
    }

    @Test
    public void testEncryption() throws Exception {
        String password ="admin";
        String enc=PasswordEncryptionService.encrypt(password);
        assertNotEquals(password,enc);
        assertEquals(password,PasswordEncryptionService.decrypt(enc));
    }

    @Test
    void add_row_and_exists() {
        DataTable table= new DataTable(List.of("id", "name"));
        table.addRow(new Object[]{1, "Teodor"});
        assertEquals(1, table.getRows().size());
        CSVExporter exporter = new CSVExporter();
        try {
           exporter.export(table, Path.of("DataExplorer"));
           assertTrue(Files.exists(Path.of("DataExplorer")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}