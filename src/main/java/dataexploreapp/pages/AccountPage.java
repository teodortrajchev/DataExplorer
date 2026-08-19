package dataexploreapp.pages;

import dataexploreapp.auth.AuthService;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.save.ConnectionManager;
import dataexploreapp.db_config.save.SavedConnection;
import dataexploreapp.db_config.save.SavedConnectionService;
import dataexploreapp.dialogs.LoginDialog;
import dataexploreapp.dialogs.UpdateConnectionDialog;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class AccountPage {

    private final String username;
    private final DataBaseReader currentReader;
    private final String currentSchema;
    private static final Dotenv dotenv = Dotenv.load();

    private final Button updateButton = new Button("Update Database");
    private final Button homeButton = new Button("Home");
    private final Button logoutBtn = new Button("Logout");
    private Stage stage;

    public AccountPage(String username, DataBaseReader currentReader, String currentSchema) {
        this.username = username;
        this.currentReader = currentReader;
        this.currentSchema = currentSchema;
    }


    public void show(Stage stage) throws SQLException {
        this.stage = stage;

        String sql = "SELECT created_at FROM "+dotenv.get("APP_USERS_TABLE")+" WHERE username = '" + username + "'";

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(getSavedConnections());
        Button setdefault=new Button("Set Default");
        setdefault.setOnAction(e -> {
           String v=comboBox.getValue();
            try {
                SavedConnectionService.change_default(v);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        setdefault.setText("Set Default Connection");
        setdefault.getStyleClass().add("primary-button");
        java.util.List<Object[]> rows = currentReader.runQuery(sql).getRows();
        String created_at = "";
        if (!rows.isEmpty()) {
            Object value = rows.get(0)[0];
            created_at = value == null ? "" : value.toString();
        }

        HBox navbar = buildNavbar();

        Label title = new Label(username);
        title.getStyleClass().add("welcome-title");

        updateButton.setText("Update Connection");
        updateButton.getStyleClass().add("primary-button");

        updateButton.setOnAction(e -> new UpdateConnectionDialog(username, currentReader, currentSchema).show(stage));

        // Username
        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle("-fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #777777;");

        title.setStyle("-fx-font-size: 26px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #222222;");

        VBox usernameBox = new VBox(5, usernameLabel, title);

        // Created at
        Label createdLabel = new Label("Account created");
        createdLabel.setStyle("-fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #777777;");

        Label createdAtValue = new Label(created_at);
        createdAtValue.setStyle("-fx-font-size: 15px;" + "-fx-text-fill: #555555;");

        VBox createdBox = new VBox(5, createdLabel, createdAtValue);

        VBox information = new VBox(25, usernameBox, createdBox, updateButton,comboBox,setdefault);


        information.setAlignment(Pos.CENTER_LEFT);
        information.setPadding(new Insets(30));

        information.setMaxWidth(500);

        information.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e2e2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;");

        // Center
        StackPane center = new StackPane(information);

        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(50));

        center.setStyle("-fx-background-color: #f5f6f8;");
        BorderPane root = new BorderPane();
        root.setTop(navbar);
        root.setCenter(center);

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

        String navBtnStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;";


        homeButton.setStyle(navBtnStyle);
        logoutBtn.setStyle(navBtnStyle);
        homeButton.setOnAction(e -> new DataBrowserPage(username, currentReader, currentSchema).show(stage));
        logoutBtn.setOnAction(e -> {
            new LoginDialog(new AuthService()).show(new Stage());
            stage.close();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(homeButton, spacer,logoutBtn);
        return navbar;
    }
    private List<String> getSavedConnections(){
        List<SavedConnection> conns=SavedConnectionService.loadConnections();
        List<String> con_names=new ArrayList<>();
        for(SavedConnection s: conns){
            con_names.add(s.getName());
        }
        return con_names;
    }



}
