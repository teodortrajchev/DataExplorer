package DataExportApp.Pages;

import DataExportApp.Auth.AuthService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LoginView {

    private final AuthService userDao;
    private Stage stage;

    private final TextField userIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = new Label();
    private final Button loginButton = new Button("Login");
    private final Button resetButton = new Button("Reset");

    public LoginView(AuthService userDao) {
        this.userDao = userDao;
    }

    public void show(Stage stage) {
        this.stage = stage;

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(30));

        grid.add(new Label("User ID:"), 0, 0);
        grid.add(userIdField, 1, 0);

        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        HBox buttonBox = new HBox(10, loginButton, resetButton);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 0, 2, 2, 1);

        messageLabel.setStyle("-fx-font-style: italic; -fx-font-size: 14px;");
        grid.add(messageLabel, 0, 3, 2, 1);

        loginButton.setOnAction(e -> attemptLogin());
        resetButton.setOnAction(e -> {
            userIdField.clear();
            passwordField.clear();
            messageLabel.setText("");
        });

        passwordField.setOnAction(e -> attemptLogin());

        Scene scene = new Scene(grid, 380, 260);
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    private void attemptLogin() {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        loginButton.setDisable(true);
        messageLabel.setTextFill(Color.GRAY);
        messageLabel.setText("Checking...");

        Task<AuthService.AuthResult> authTask = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                return userDao.authenticate(userId, password);
            }
        };

        authTask.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            handleResult(authTask.getValue(), userId);
        });

        authTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Unexpected error");
            authTask.getException().printStackTrace();
        });

        Thread thread = new Thread(authTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleResult(AuthService.AuthResult result, String userId) {
        switch (result) {
            case SUCCESS -> {
                messageLabel.setTextFill(Color.GREEN);
                messageLabel.setText("Login Successful");
                new WelcomePage(userId).show(new Stage());
                stage.close();
            }
            case WRONG_PASSWORD -> {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Login Failed");
            }
            case USER_NOT_FOUND -> {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Username not found");
            }
            case DB_ERROR -> {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Database error");
            }
        }
    }
}