package auth;

import dataexploreapp.auth.AuthService;
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

public class AuthServiceTest {
    private AuthService authService=new AuthService();
    @Test
    void login_valid_test() {

        AuthService.AuthResult result = authService.authenticate("admin", "admin");
        assertEquals(AuthService.AuthResult.SUCCESS,result);
    }
    @Test
    void login_invalid_password_test() {
        AuthService.AuthResult result = authService.authenticate("admin", "wrong");

        assertEquals(AuthService.AuthResult.WRONG_PASSWORD,result);
    }
    @Test
    void login_invalid_user_test() {
        AuthService.AuthResult result = authService.authenticate("admin2", "admin");
        assertEquals(AuthService.AuthResult.USER_NOT_FOUND,result);
    }

//    PasswordEncryptionService
    @Test
    void encrypt_decrypt_test() {
        String password = "admin123";
        String encrypted = null;
        try {
            encrypted = PasswordEncryptionService.encrypt(password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String decrypted = null;
        try {
            decrypted = PasswordEncryptionService.decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotEquals(password,encrypted);
        assertEquals(password, decrypted);
    }

//    different encryption for each instance
    @Test
    void encrypt_shouldProduceDifferentCiphertext() {
        String password = "admin123";
        String encrypted1 = null;
        try {
            encrypted1 = PasswordEncryptionService.encrypt(password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String encrypted2 = null;
        try {
            encrypted2 = PasswordEncryptionService.encrypt(password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertNotEquals(encrypted1, encrypted2);
    }
    @Test
    void decrypt_invalid_ciphertext_shouldFail() {
        assertThrows(Exception.class, () -> PasswordEncryptionService.decrypt("invalid-data"));
    }
}
