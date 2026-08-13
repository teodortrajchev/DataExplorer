package dataexploreapp.dialogs;

import dataexploreapp.db_config.database.ProcedureParameter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProceduresParamsDialog {

    public static Optional<Map<String, String>> show(Stage owner, String procedureName, List<ProcedureParameter> inputParams) {
        Stage dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(procedureName + " — parameters");

        Label title = new Label("Enter parameters for " + procedureName);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        Map<String, TextField> fields = new LinkedHashMap<>();
        int row = 0;
        for (ProcedureParameter param : inputParams) {
            Label label = new Label(param.getName() + ":");
            TextField field = new TextField();
            field.setPromptText(param.getDataType());
            fields.put(param.getName(), field);
            grid.add(label, 0, row);
            grid.add(field, 1, row);
            row++;
        }

        // Holds the result across the button's lambda, since showAndWait() blocks
        // until dialogStage.close() is called from one of the button handlers below.
        @SuppressWarnings("unchecked")
        Map<String, String>[] result = new Map[]{null};

        Button runBtn = new Button("Run");
        runBtn.getStyleClass().add("primary-button");
        Button cancelBtn = new Button("Cancel");

        runBtn.setOnAction(e -> {
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, TextField> entry : fields.entrySet()) {
                values.put(entry.getKey(), entry.getValue().getText());
            }
            result[0] = values;
            dialogStage.close();
        });
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttons = new HBox(10, runBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, title, grid, buttons);
        root.setPadding(new Insets(20));

        dialogStage.setScene(new Scene(root, 380, 120 + inputParams.size() * 40));
        dialogStage.showAndWait();

        return Optional.ofNullable(result[0]);
    }
}