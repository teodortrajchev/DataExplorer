package dataexploreapp.pages;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.dialogs.UpdateConnectionDialog;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AccountPage {

    private final String username;
    private final DataBaseReader currentReader;
    private final String currentSchema;


    private final Button updateButton = new Button("Update Database");
    private final Button homeButton = new Button("Home");

    //    private final String userId;
    private Stage stage;

    public AccountPage(String username, DataBaseReader currentReader, String currentSchema) {
        this.username = username;
        this.currentReader = currentReader;
        this.currentSchema = currentSchema;
    }

    public void show(Stage stage) {
        this.stage = stage;

        HBox navbar = buildNavbar();

        Label title = new Label(username);
        title.getStyleClass().add("welcome-title");
        updateButton.setOnAction(e -> new UpdateConnectionDialog(username, currentReader, currentSchema).show(stage));

        BorderPane root = new BorderPane(updateButton);
        root.setTop(navbar);

        Scene scene = new Scene(root, 1530, 800);
        var cssUrl = getClass().getResource("welcome.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Settings");
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
        homeButton.setOnAction(e -> new DataBrowserPage(username, currentReader, currentSchema).show(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(navLabel, spacer,homeButton);
        return navbar;
    }


}
