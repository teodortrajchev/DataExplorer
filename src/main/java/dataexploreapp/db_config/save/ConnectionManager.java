package dataexploreapp.db_config.save;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.encryption.PasswordEncryptionService;
import dataexploreapp.pages.ConnectionPage;
import dataexploreapp.pages.DataBrowserPage;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ConnectionManager {

    private ConnectionManager() {
    }

    public static void open(String username, Stage stage) {

        Optional<SavedConnection> defaultConnection = SavedConnectionService.getDefaultConnection(username);
        System.out.println(defaultConnection.isPresent());
        if (defaultConnection.isEmpty()) {

            new ConnectionPage(username).show(stage);

            return;
        }

        connectToSavedConnection(username, stage, defaultConnection.get());
    }

    private static void connectToSavedConnection(String username, Stage stage, SavedConnection savedConnection) {

        Task<DataBaseReader> task = new Task<>() {

            @Override
            protected DataBaseReader call() throws Exception {

                String password = PasswordEncryptionService.decrypt(savedConnection.getEncryptedPassword());
                String dbuser = PasswordEncryptionService.decrypt(savedConnection.getDatabaseUser());

                DataBaseReader reader = new DataBaseReader(savedConnection.getJdbcUrl(), dbuser, password);

                reader.testConnection();

                return reader;
            }
        };

        task.setOnSucceeded(e -> {new DataBrowserPage(username, task.getValue(), savedConnection.getSchema()).show(stage);});

        task.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "The default database connection could not be established.\n\n" + "You can connect manually or try again.", ButtonType.OK
            );

            alert.setTitle("Database Connection Failed");
            alert.setHeaderText("Could not connect to \"" + savedConnection.getName() + "\"");

            alert.showAndWait();

            new ConnectionPage(username).show(stage);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}