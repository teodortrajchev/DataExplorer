package dataexploreapp.auth;

import dataexploreapp.dialogs.LoginDialog;
import javafx.application.Application;
import javafx.stage.Stage;

public class LoginService extends Application {

    @Override
    public void start(Stage primaryStage) {
        AuthService userDao = new AuthService();

        LoginDialog loginDialog = new LoginDialog(userDao);
        loginDialog.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}