package DataExportApp.Auth;

import DataExportApp.Pages.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class LoginService extends Application {

    @Override
    public void start(Stage primaryStage) {
        AuthService userDao = new AuthService();

        LoginView loginView = new LoginView(userDao);
        loginView.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}