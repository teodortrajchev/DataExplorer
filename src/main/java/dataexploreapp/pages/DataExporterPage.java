package dataexploreapp.pages;

import dataexploreapp.auth.AuthService;
import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dialogs.LoginDialog;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.dataexport.exporters.ExportFormat;
import dataexploreapp.db_config.validation.SQLValidator;

import java.io.File;

public class DataExporterPage {

    public void show(Stage stage) {
        HBox navbar = new HBox(12);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2b2b2b;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(spacer, closeBtn);



        Button homeBtn = new Button("Home");
        Button exporterbtn = new Button("Data Exporter");
        Button logoutBtn = new Button("Logout");
        AuthService userDao = new AuthService();
        logoutBtn.setOnAction(e ->
        {new LoginDialog(userDao).show(new Stage());
            stage.close();});
        exporterbtn.setOnAction(e ->
        {new DataExporterPage().show(new Stage());});

        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(spacer, homeBtn, exporterbtn, logoutBtn);
        Label title = new Label("Data Exporter");
        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        Label urlLabel = new Label("JDBC URL");

        TextField urlField = new TextField();
        urlField.setPromptText("");


        Label usernameLabel = new Label("Username");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Database username");


        Label passwordLabel = new Label("Password");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Database password");
        Button testConnectionButton=new Button("Test Connection");

        Label sqlStatus = new Label("Not validated");
        Label queryLabel = new Label("SQL Query");

        TextArea queryArea = new TextArea();
        queryArea.setPromptText(
                "SELECT * FROM employees"
        );

        queryArea.setPrefRowCount(6);
        queryArea.setWrapText(true);

        Label formatLabel = new Label("Export Format");

        ComboBox<ExportFormat> formatBox = new ComboBox<>();

        formatBox.getItems().addAll(
                ExportFormat.values()
        );

        formatBox.setValue(ExportFormat.XLSX);
        Label outputLabel = new Label("Output File");

        TextField outputField = new TextField();
        outputField.setPromptText("Choose output file...");

        Button browseButton = new Button("Browse");

        HBox outputBox = new HBox(
                10,
                outputField,
                browseButton
        );

        HBox.setHgrow(
                outputField,
                Priority.ALWAYS
        );


        Label documentTitleLabel = new Label("Document Title");
        TextField documentTitleField = new TextField("Exported Data");
        Label statusLabel = new Label("Status: Ready");


        browseButton.setOnAction(e -> {

            ExportFormat format = formatBox.getValue();

            if (format == null) {
                return;
            }

            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle("Save Exported File");
            fileChooser.setInitialFileName( documentTitleField.getText().trim());
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            format.toString() + " (*" + format.getExtension() + ")",
                            "*" + format.getExtension()
                    )
            );

            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {

                String path = file.getAbsolutePath();

                // Automatically add extension
                if (!path.toLowerCase().endsWith(
                        format.getExtension())) {

                    path += format.getExtension();
                }

                outputField.setText(path);
            }
        });


        Button exportButton = new Button("Export");

        exportButton.setPrefWidth(150);
        exportButton.setPrefHeight(40);

        exportButton.setStyle(
                "-fx-font-size: 16px;"
        );


        exportButton.setOnAction(e -> {

            String jdbcUrl = urlField.getText().trim();

            String username = usernameField.getText().trim();

            String password = passwordField.getText();

            String query = queryArea.getText().trim();

            String output = outputField.getText().trim();

            String documentTitle = documentTitleField.getText().trim();

            ExportFormat format = formatBox.getValue();
            try {

                SQLValidator.validate(query);

                sqlStatus.setText(
                        "✓ SQL query is valid"
                );

                sqlStatus.setStyle(
                        "-fx-text-fill: green;" +
                                "-fx-font-weight: bold;"
                );

            } catch (IllegalArgumentException ex) {

                sqlStatus.setText(
                        "✗ " + ex.getMessage()
                );

                sqlStatus.setStyle(
                        "-fx-text-fill: red;" +
                                "-fx-font-weight: bold;"
                );
                return;
            }

            if (jdbcUrl.isEmpty()
                    || username.isEmpty()
                    || query.isEmpty()
                    || output.isEmpty()) {

                statusLabel.setText("Status: Please fill in all required fields.");

                return;
            }


            statusLabel.setText(
                    "Status: Exporting..."
            );

            exportButton.setDisable(true);


            Thread thread = new Thread(() -> {

                try {

                    DataExportService service = new DataExportService();

                    service.export(
                            jdbcUrl,
                            username,
                            password,
                            query,
                            format,
                            new java.io.File(output).toPath(),
                            documentTitle
                    );


                    javafx.application.Platform.runLater(() -> {

                        statusLabel.setText(
                                "Status: Export completed!"
                        );

                        statusLabel.setStyle(
                                "-fx-text-fill: green; " +
                                        "-fx-font-weight: bold;"
                        );

                        exportButton.setDisable(false);
                        showCompletedDialog(stage,output);

                    });


                } catch (Exception ex) {

                    javafx.application.Platform.runLater(() -> {

                        statusLabel.setText(
                                "Status: Export failed - " + ex.getMessage()
                        );

                        statusLabel.setStyle(
                                "-fx-text-fill: red; " +
                                        "-fx-font-weight: bold;"
                        );

                        exportButton.setDisable(false);

                    });

                    ex.printStackTrace();
                }

            });

            thread.setDaemon(true);
            thread.start();
        });
        testConnectionButton.setOnAction(e -> {

            statusLabel.setText("Connecting...");

            Task<Void> task = new Task<>() {

                @Override
                protected Void call() throws Exception {

                    DataBaseReader reader = new DataBaseReader(
                            urlField.getText().trim(),
                            usernameField.getText().trim(),
                            passwordField.getText()
                    );

                    reader.testConnection();

                    return null;
                }
            };

            task.setOnSucceeded(event -> {

                statusLabel.setText("✓ Connection successful");

            });

            task.setOnFailed(event -> {
                statusLabel.setText("✗ Connection failed: " + task.getException().getMessage());
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        VBox content = new VBox(10);

        content.setPadding(
                new Insets(30)
        );

        content.getChildren().addAll(

                title,

                new Separator(),

                urlLabel,
                urlField,

                usernameLabel,
                usernameField,

                passwordLabel,
                passwordField,

                testConnectionButton,
                statusLabel,

                queryLabel,
                queryArea,
                sqlStatus,

                formatLabel,
                formatBox,

                outputLabel,
                outputBox,

                documentTitleLabel,
                documentTitleField,

                new Separator(),

                exportButton
        );
        testConnectionButton.setOnAction(e -> {

            statusLabel.setText("Connecting...");
            statusLabel.setStyle("-fx-text-fill: orange;");

            Task<Void> task = new Task<>() {

                @Override
                protected Void call() throws Exception {

                    DataBaseReader reader =
                            new DataBaseReader(
                                    urlField.getText().trim(),
                                    usernameField.getText().trim(),
                                    passwordField.getText()
                            );

                    reader.testConnection();

                    return null;
                }
            };

            task.setOnSucceeded(event -> {

                statusLabel.setText(
                        "✓ Connection successful"
                );

                statusLabel.setStyle(
                        "-fx-text-fill: green; " +
                                "-fx-font-weight: bold;"
                );
            });

            task.setOnFailed(event -> {

                Throwable error = task.getException();

                statusLabel.setText(
                        "✗ Connection failed: "
                                + error.getMessage()
                );

                statusLabel.setStyle(
                        "-fx-text-fill: red; " +
                                "-fx-font-weight: bold;"
                );
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        ScrollPane scrollPane = new ScrollPane(content);

        scrollPane.setFitToWidth(true);


        BorderPane root = new BorderPane();

        root.setCenter(scrollPane);


        Scene scene = new Scene(root, 1000, 750);
        var cssUrl = getClass().getResource("global_dark.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Data Exporter");
        stage.setScene(scene);
        stage.show();
    }


    private void showCompletedDialog(Stage owner, String outputPath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Export Completed");
        alert.setHeaderText("✓ Export completed successfully");

        alert.setContentText("Your file has been created successfully.\n\n" + outputPath);

        alert.showAndWait();
    }
}