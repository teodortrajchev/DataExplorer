package dataexploreapp.dialogs;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.pages.AccountPage;
import dataexploreapp.pages.DataBrowserPage;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UpdateConnectionDialog {

    private final String username;
    private final DataBaseReader currentReader;
    private final String currentSchema;

    private final TextField urlField = new TextField();
    private final TextField dbUserField = new TextField();
    private final PasswordField dbPasswordField = new PasswordField();
    private final TextField schemaField = new TextField();
    private final Label statusLabel = new Label();
    private final Button connectButton = new Button("Connect");
    private final Button cancelButton = new Button("Cancel");
    private Stage ownerStage;
    public UpdateConnectionDialog(String username, DataBaseReader currentReader, String currentSchema) {
        this.username = username;
        this.currentReader = currentReader;
        this.currentSchema = currentSchema;
    }
    public void show(Stage ownerStage) {
        Stage dialogStage = new Stage();
        this.ownerStage=ownerStage;
        dialogStage.initOwner(ownerStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        Label title = new Label("Update your connection details, or connect to a different database entirely.");
        title.getStyleClass().add("welcome-subtitle");

        // Prefill everything except password — never re-display a stored credential.
        urlField.setText(currentReader.getJdbcUrl());
        dbUserField.setText(currentReader.getUsername());
        schemaField.setText(currentSchema);

        urlField.setPromptText("jdbc:oracle:thin:@//host:port/service_name");
        dbUserField.setPromptText("Database username");
        dbPasswordField.setPromptText("Database password");
        schemaField.setPromptText("Schema to browse (defaults to username)");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);
        form.add(new Label("JDBC URL:"), 0, 0);
        form.add(urlField, 1, 0);
        form.add(new Label("Username:"), 0, 1);
        form.add(dbUserField, 1, 1);
        form.add(new Label("Password:"), 0, 2);
        form.add(dbPasswordField, 1, 2);
        form.add(new Label("Schema:"), 0, 3);
        form.add(schemaField, 1, 3);
        urlField.setPrefWidth(300);
        dbUserField.setPrefWidth(300);
        dbPasswordField.setPrefWidth(300);
        schemaField.setPrefWidth(300);

        connectButton.getStyleClass().add("primary-button");
        connectButton.setOnAction(e -> attemptReconnect(dialogStage));
        cancelButton.setOnAction(e -> dialogStage.close());
        statusLabel.setStyle("-fx-font-style: italic;");

        HBox buttons = new HBox(10, connectButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(16, title, form, buttons, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("welcome-center");


        Scene scene = new Scene(root, 600, 400);

        var cssUrl = getClass().getResource("welcome.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        dialogStage.setTitle("Update Connection");
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }


    private void attemptReconnect(Stage dialogstage) {
        String url = urlField.getText().trim();
        String dbUser = dbUserField.getText().trim();
        String dbPassword = dbPasswordField.getText();

        if (url.isEmpty() || dbUser.isEmpty()) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("JDBC URL and username are required.");
            return;
        }
        String schema = schemaField.getText().trim().isEmpty() ? dbUser : schemaField.getText().trim();

        connectButton.setDisable(true);
        cancelButton.setDisable(true);
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setText("Connecting...");

        Task<DataBaseReader> connectTask = new Task<>() {
            @Override
            protected DataBaseReader call() throws Exception {
                // Constructing DataBaseReader is what actually connects (HikariCP
                // validates connectivity eagerly in its constructor), so it has to
                // happen inside the Task — not before it's created — or a bad
                // connection throws on the JavaFX Application Thread before
                // setOnFailed exists to catch it.
                DataBaseReader newReader = new DataBaseReader(url, dbUser, dbPassword);
                newReader.testConnection();
                return newReader;
            }
        };

        connectTask.setOnSucceeded(e -> {
            connectButton.setDisable(false);
            cancelButton.setDisable(false);

            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Connected!");

            dialogstage.close();

            new DataBrowserPage(
                    username,
                    connectTask.getValue(),
                    schema
            ).show(ownerStage);
        });

        connectTask.setOnFailed(e -> {
            connectButton.setDisable(false);
            cancelButton.setDisable(false);
            statusLabel.setTextFill(Color.RED);
            Throwable ex = connectTask.getException();
            statusLabel.setText("Connection failed: " + (ex != null ? ex.getMessage() : "unknown error"));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(connectTask).start();
    }



}
