package DataExportApp.Pages;

import DataExportApp.Auth.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class WelcomePage {

    private final String username;

    public WelcomePage(String username) {
        this.username = username;
    }

    public void show(Stage stage) {

        HBox navbar = new HBox(12);
        navbar.setPadding(new Insets(10, 20, 10, 20));
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setStyle("-fx-background-color: #2b2b2b;");

        Button homeBtn = new Button("Home");
        Button exporterBtn = new Button("Data Exporter");
        Button logoutBtn = new Button("Logout");

        String[] navButtonStyle = {
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;"
        };
        homeBtn.setStyle(navButtonStyle[0]);
        exporterBtn.setStyle(navButtonStyle[0]);
        logoutBtn.setStyle(navButtonStyle[0]);

        AuthService userDao = new AuthService();

        logoutBtn.setOnAction(e -> {
            new LoginView(userDao).show(new Stage());
            stage.close();
        });
        exporterBtn.setOnAction(e -> new DataExporterPage().show(new Stage()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label navLabel = new Label("Welcome, " + username + "!");
        navLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        navbar.getChildren().addAll(navLabel, spacer, homeBtn, exporterBtn, logoutBtn);

        Label titleLabel = new Label("Welcome, " + username + "!");
        titleLabel.getStyleClass().add("welcome-title");

        Label subtitleLabel = new Label("Export your database queries to Excel, Word, PDF, and more.");
        subtitleLabel.getStyleClass().add("welcome-subtitle");

        Button exportButton = new Button("Export Data");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setOnAction(e -> new DataExporterPage().show(new Stage()));

        VBox centerContent = new VBox(16, titleLabel, subtitleLabel, exportButton);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.getStyleClass().add("welcome-center");

        BorderPane root = new BorderPane();
        root.setTop(navbar);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1400, 700);
        scene.getStylesheets().add(getClass().getResource("/DataExportApp/Pages/welcome.css").toExternalForm());

        stage.setTitle("Welcome");
        stage.setScene(scene);
        stage.show();
    }
}