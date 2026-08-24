package validation;
import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.db_config.validation.SQLValidator;
import dataexploreapp.encryption.PasswordEncryptionService;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
public class SQLValidatorTest {

    @Test
    void empty_query_test() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {SQLValidator.validate("");});
    }
    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM USERS","CALL GET_ALL_USERS(?)","{CALL GET_ALL_USERS(?)}","BEGIN NULL; END;",})
    void valid_query_test(String query) {
        assertDoesNotThrow(() -> {SQLValidator.validate(query);});
    }

    @ParameterizedTest
    @ValueSource(strings = {"DROP TABLE users", "DROP DATABASE test", " DELETE FROM users", "TRUNCATE TABLE users" , "ALTER TABLE users ADD test VARCHAR2(10)" , "CREATE TABLE test (id NUMBER)" , "CREATE USER test IDENTIFIED BY password" , "GRANT SELECT ON users TO test" , "REVOKE SELECT ON users FROM test",})
    void invalid_query_test(String query) {
        assertThrows(IllegalArgumentException.class,() -> {SQLValidator.validate(query);});
    }
}
