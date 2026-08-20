package dataexploreapp.pages;

import dataexploreapp.auth.AuthService;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.save.ConnectionManager;
import dataexploreapp.db_config.save.SavedConnection;
import dataexploreapp.db_config.save.SavedConnectionService;
import dataexploreapp.dialogs.LoginDialog;
import dataexploreapp.dialogs.UpdateConnectionDialog;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        comboBox.setPromptText("Choose a saved connection");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        Button setdefault=new Button("Set Default");
        StackPane checkAnimation = createCheckAnimation();
        checkAnimation.setVisible(false);
        checkAnimation.setManaged(false);
        setdefault.setOnAction(e -> {
            String v = comboBox.getValue();
            try {
                SavedConnectionService.change_default(v);
                playCheckAnimation(checkAnimation);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        setdefault.setText("Set Default Connection");
        setdefault.getStyleClass().add("primary-button");
        setdefault.setMaxWidth(Double.MAX_VALUE);
        java.util.List<Object[]> rows = currentReader.runQuery(sql).getRows();
        String created_at = "";
        if (!rows.isEmpty()) {
            Object value = rows.get(0)[0];
            created_at = value == null ? "" : value.toString();
        }

        HBox navbar = buildNavbar();

        Label title = new Label(username);
        title.getStyleClass().add("welcome-title");
        title.setStyle("-fx-font-size: 26px;");

        updateButton.setText("Update Connection");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setMaxWidth(Double.MAX_VALUE);

        updateButton.setOnAction(e -> new UpdateConnectionDialog(username, currentReader, currentSchema).show(stage));

        // Username
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("muted-label");

        VBox usernameBox = new VBox(5, usernameLabel, title);

        // Created at
        Label createdLabel = new Label("Account created");
        createdLabel.getStyleClass().add("muted-label");

        Label createdAtValue = new Label(created_at);
        createdAtValue.getStyleClass().add("section-label");

        VBox createdBox = new VBox(5, createdLabel, createdAtValue);

        Separator divider = new Separator();
        divider.getStyleClass().add("card-divider");

        Label connectionSectionLabel = new Label("Saved connection");
        connectionSectionLabel.getStyleClass().add("section-label");

        HBox defaultRow = new HBox(10, setdefault, checkAnimation);
        defaultRow.setAlignment(Pos.CENTER_LEFT);
        VBox connectionBox = new VBox(10, connectionSectionLabel, comboBox, defaultRow);
        VBox information = new VBox(22, usernameBox, createdBox, divider, updateButton, connectionBox);

        information.setAlignment(Pos.CENTER_LEFT);
        information.setPadding(new Insets(30));

        information.setMaxWidth(420);
        information.setMaxHeight(Region.USE_PREF_SIZE);

        information.getStyleClass().add("panel-card");

        // Center
        StackPane center = new StackPane(information);

        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(50));

        BorderPane root = new BorderPane();
        root.setTop(navbar);
        root.setCenter(center);

        Scene scene = new Scene(root, 1530, 800);

        var cssUrl = getClass().getResource("global_dark.css");
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
        navbar.getStyleClass().add("navbar");

        homeButton.getStyleClass().add("nav-button");
        logoutBtn.getStyleClass().add("nav-button");
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

    private StackPane createCheckAnimation() {

        Circle circle = new Circle(14);
        circle.setFill(Color.web("#22c55e"));

        Path check = new Path(
                new MoveTo(-7, 0),
                new LineTo(-2, 5),
                new LineTo(7, -6)
        );

        check.setFill(null);
        check.setStroke(Color.WHITE);
        check.setStrokeWidth(2.5);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);

        check.getStrokeDashArray().addAll(30.0, 30.0);
        check.setStrokeDashOffset(30);

        StackPane container = new StackPane(circle, check);

        container.setMinSize(28, 28);
        container.setPrefSize(28, 28);
        container.setMaxSize(28, 28);

        return container;
    }


    private void playCheckAnimation(StackPane container) {

        container.setManaged(true);
        container.setVisible(true);

        container.setOpacity(0);
        container.setScaleX(0.6);
        container.setScaleY(0.6);

        Path check = (Path) container.getChildren().get(1);

        check.setStrokeDashOffset(30);

        FadeTransition fade = new FadeTransition(Duration.millis(180), container);

        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(350), container);

        scale.setFromX(0.6);
        scale.setFromY(0.6);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        Timeline drawCheck = new Timeline(
                new KeyFrame(Duration.ZERO,
                new KeyValue(check.strokeDashOffsetProperty(), 30)),
                new KeyFrame(Duration.millis(350),
                        new KeyValue(check.strokeDashOffsetProperty(), 0, Interpolator.EASE_OUT)));

        ParallelTransition animation = new ParallelTransition(fade, scale, drawCheck);

        animation.play();
    }

}