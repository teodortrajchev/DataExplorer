package db;

import dataexploreapp.db_config.save.SavedConnection;
import dataexploreapp.db_config.save.SavedConnectionService;
import dataexploreapp.encryption.PasswordEncryptionService;
import io.github.cdimascio.dotenv.Dotenv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SavedConnectionTest {
    private static final Dotenv dotenv = Dotenv.load();
    @Test
    void savedConnection_value_test() throws Exception {

        String applicationUser = "admin";
        String databaseUser = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASS");

        String encryptedApplicationUser = PasswordEncryptionService.encrypt(applicationUser);

        String encryptedPassword = PasswordEncryptionService.encrypt(password);

        SavedConnection connection = new SavedConnection("DBTest", encryptedApplicationUser, dotenv.get("DB_URL"), databaseUser, encryptedPassword, "SYSTEM", true);

        assertEquals("DBTest", connection.getName());
        assertEquals("admin", connection.getApplicationUser());
        assertEquals(dotenv.get("DB_URL"), connection.getJdbcUrl());
        assertEquals("system", connection.getDatabaseUser());
        assertEquals(encryptedPassword, connection.getEncryptedPassword());
        assertEquals("SYSTEM", connection.getSchema());
        assertTrue(connection.isDefaultConnection());
    }
    @Test
    void password_is_encrypted_test() throws Exception {
        String password = "secret";
        String encrypted = PasswordEncryptionService.encrypt(password);

        SavedConnection connection = new SavedConnection("Test", PasswordEncryptionService.encrypt("Teodor"), "jdbc:test", "SYSTEM", encrypted, "SYSTEM", false);
        assertNotEquals(password, connection.getEncryptedPassword());
    }


    @Test
    void saveConnection_test() throws Exception {
        SavedConnection connection = createConnection("DBTEST", "Teodor", false);

        SavedConnectionService.saveConnection(connection);

        List<SavedConnection> connections = SavedConnectionService.loadConnections();
        assertEquals(1, connections.size());
        SavedConnection saved = connections.get(0);

        assertEquals("DBTEST", saved.getName());
        assertEquals("Teodor", saved.getApplicationUser());
        assertEquals("jdbc:test", saved.getJdbcUrl());
    }

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        SavedConnectionService.setConnectionsFile(tempDir.resolve("connections.json"));
    }

    private SavedConnection createConnection(String name, String user, boolean defaultConnection) throws Exception {
        return new SavedConnection(
                name,
                PasswordEncryptionService.encrypt(user),
                "jdbc:test",
                PasswordEncryptionService.encrypt("SYSTEM"),
                PasswordEncryptionService.encrypt("password"),
                "SYSTEM",
                defaultConnection
        );
    }

    @Test
    void getConnectionsForUser_test() throws Exception {

        SavedConnection teo = createConnection("Teo DB", "Teodor", false);
        SavedConnection mila = createConnection("Mila DB", "Mila", false);

        SavedConnectionService.saveConnection(teo);
        SavedConnectionService.saveConnection(mila);

        List<SavedConnection> result = SavedConnectionService.getConnectionsForUser("teodor");

        assertEquals(1, result.size());
        assertEquals("Teo DB", result.get(0).getName());
    }


    @Test
    void getDefaultConnection_test() throws Exception {

        SavedConnection normal = createConnection("Normal", "Teodor", false);

        SavedConnection defaultConnection = createConnection("Default", "Teodor", true);

        SavedConnectionService.saveConnection(normal);
        SavedConnectionService.saveConnection(defaultConnection);

        var result = SavedConnectionService.getDefaultConnection("Teodor");

        assertTrue(result.isPresent());
        assertEquals("Default", result.get().getName());
    }

    @Test
    void saveConnection_replaceConnection() throws Exception {
        SavedConnection first = createConnection("My DB", "Teodor", false);
        SavedConnection replacement = createConnection("My DB", "Teodor", true);

        SavedConnectionService.saveConnection(first);
        SavedConnectionService.saveConnection(replacement);

        List<SavedConnection> connections = SavedConnectionService.loadConnections();

        assertEquals(1, connections.size());
        assertTrue(connections.get(0).isDefaultConnection());
    }

//Da ne se zameni konekcija koja shto ima isto ime a razlicen user
    @Test
    void sameNameForDifferentUsers_replace_test() throws Exception {
        SavedConnection teo = createConnection("My DB", "Teodor", false);
        SavedConnection mila = createConnection("My DB", "Mila", false);

        SavedConnectionService.saveConnection(teo);
        SavedConnectionService.saveConnection(mila);

        List<SavedConnection> connections = SavedConnectionService.loadConnections();
        assertEquals(2, connections.size());
    }

    @Test
    void deleteConnection_test() throws Exception {

        SavedConnection connection = createConnection("My DB", "Teodor", false);
        SavedConnectionService.saveConnection(connection);
        SavedConnectionService.deleteConnection("Teodor", "My DB");

        List<SavedConnection> connections = SavedConnectionService.loadConnections();

        assertTrue(connections.isEmpty());
    }
}