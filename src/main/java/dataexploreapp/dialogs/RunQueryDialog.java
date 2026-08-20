package dataexploreapp.dialogs;

import dataexploreapp.db_config.validation.SQLValidator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class RunQueryDialog {

    public static Optional<String> show(Stage owner) {
        Stage dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle("Run query");

        Label title = new Label("Write a query");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label hint = new Label("SELECT statements and stored procedure calls only.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        TextArea queryArea = new TextArea();
        queryArea.setPromptText("SELECT or CALL ");
        queryArea.setPrefRowCount(8);
        queryArea.setPrefColumnCount(50);
        queryArea.setWrapText(true);
        queryArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(480);

        Optional<String>[] result = new Optional[]{Optional.empty()};

        Button runBtn = new Button("Run");
        runBtn.getStyleClass().add("primary-button");
        Button cancelBtn = new Button("Cancel");

        Runnable attemptRun = () -> {
            String query = queryArea.getText();
            try {
                SQLValidator.validate(query);
                result[0] = Optional.of(query);
                dialogStage.close();
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
            }
        };

        runBtn.setOnAction(e -> attemptRun.run());
        cancelBtn.setOnAction(e -> dialogStage.close());

        // Ctrl+Enter runs the query without reaching for the mouse
        queryArea.setOnKeyPressed(e -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(e)) {
                attemptRun.run();
            }
        });

        HBox buttons = new HBox(10, runBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, hint, queryArea, errorLabel, buttons);
        root.setPadding(new Insets(20));
        Scene scene =new Scene(root, 520, 380);
        var cssUrl = RunQueryDialog.class.getResource("/dataexploreapp/pages/global.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result[0];
    }
}