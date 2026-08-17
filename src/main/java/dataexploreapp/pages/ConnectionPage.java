package dataexploreapp.pages;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.auth.AuthService;
import dataexploreapp.dialogs.LoginDialog;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ConnectionPage {

    private final String username;

    private final TextField urlField = new TextField();
    private final TextField dbUserField = new TextField();
    private final PasswordField dbPasswordField = new PasswordField();
    private final Label statusLabel = new Label();
    private final Button connectButton = new Button("Connect");
    private final TextField schemaField = new TextField();
    private Stage stage;

    public ConnectionPage(String username) {
        this.username = username;
    }

    public void show(Stage stage) {
        this.stage = stage;

        HBox navbar = buildNavbar();

        Label title = new Label("Connect to a database");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Enter your database connection details to browse and visualize its data.");
        subtitle.getStyleClass().add("welcome-subtitle");

        urlField.setPromptText("jdbc:oracle:thin:@//host:host/service_name");
        dbUserField.setPromptText("Database username");
        dbPasswordField.setPromptText("Database password");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);
        form.add(new Label("JDBC URL:"), 0, 0);
        form.add(urlField, 1, 0);
        form.add(new Label("Username:"), 0, 1);
        schemaField.setPromptText("Schema to browse (defaults to username)");
        form.add(new Label("Schema:"), 0, 3);
        form.add(schemaField, 1, 3);
        schemaField.setPrefWidth(300);
        form.add(dbUserField, 1, 1);
        form.add(new Label("Password:"), 0, 2);
        form.add(dbPasswordField, 1, 2);
        urlField.setPrefWidth(300);
        dbUserField.setPrefWidth(300);
        dbPasswordField.setPrefWidth(300);

        connectButton.getStyleClass().add("primary-button");
        connectButton.setOnAction(e -> attemptConnect());
        statusLabel.setStyle("-fx-font-style: italic;");

        VBox centerContent = new VBox(16, title, subtitle, form, connectButton, statusLabel);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(40));
        centerContent.getStyleClass().add("welcome-center");

        BorderPane root = new BorderPane();
        root.setTop(navbar);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1400, 700);
        var cssUrl = getClass().getResource("welcome.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Connect to a database");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildNavbar() {
        HBox navbar = new HBox(12);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2b2b2b;");

        Label navLabel = new Label("Welcome, " + username + "!");
        navLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            new LoginDialog(new AuthService()).show(new Stage());
            stage.close();
        });

        navbar.getChildren().addAll(navLabel, spacer, logoutBtn);
        return navbar;
    }


    private void attemptConnect() {
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
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setText("Connecting...");

        Task<DataBaseReader> connectTask = new Task<>() {
            @Override
            protected DataBaseReader call() throws Exception {
                // Constructing DataBaseReader is what actually connects — HikariCP
                // validates connectivity eagerly in its constructor — so this has
                // to run inside the Task. If it ran before the Task was created,
                // a bad connection would throw synchronously on the JavaFX
                // Application Thread, before setOnFailed even exists to catch it.
                DataBaseReader reader = new DataBaseReader(url, dbUser, dbPassword);
                reader.testConnection();
                return reader;
            }
        };

        connectTask.setOnSucceeded(e -> {
            connectButton.setDisable(false);
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setText("Connected!");
            new DataBrowserPage(username, connectTask.getValue(), schema).show(stage);
        });

        connectTask.setOnFailed(e -> {
            connectButton.setDisable(false);
            statusLabel.setTextFill(Color.RED);
            Throwable ex = connectTask.getException();
            statusLabel.setText("Connection failed: " + (ex != null ? ex.getMessage() : "unknown error"));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(connectTask).start();
    }
}