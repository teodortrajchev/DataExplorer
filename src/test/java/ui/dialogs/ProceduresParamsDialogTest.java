package ui.dialogs;

import dataexploreapp.db_config.database.ProcedureParameter;
import dataexploreapp.dialogs.ProceduresParamsDialog;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(ApplicationExtension.class)
class ProceduresParamsDialogTest {

    private Stage ownerStage;

    @Start
    private void start(Stage stage) {
        this.ownerStage = stage;
        stage.setScene(new Scene(new Pane(), 200, 100));
        stage.show();

        List<ProcedureParameter> params = List.of(
                new ProcedureParameter("EMP_ID", 1, "IN", "NUMBER"),
                new ProcedureParameter("EMP_NAME", 2, "IN", "VARCHAR2")
        );

        Platform.runLater(() -> ProceduresParamsDialog.show(ownerStage, "CREATE_EMPLOYEE", params));
    }

    @Test
    void parameters_display_test(FxRobot robot) {
        TextField firstField = robot.lookup(".text-field").nth(0).queryAs(TextField.class);
        TextField secondField = robot.lookup(".text-field").nth(1).queryAs(TextField.class);

        assertEquals("NUMBER", firstField.getPromptText());
        assertEquals("VARCHAR2", secondField.getPromptText());
    }

    @Test
    void enter_params_test(FxRobot robot) {
        TextField firstField = robot.lookup(".text-field").nth(0).queryAs(TextField.class);
        TextField secondField = robot.lookup(".text-field").nth(1).queryAs(TextField.class);

        robot.clickOn(firstField);
        robot.write("123");

        robot.clickOn(secondField);
        robot.write("Alice");

        assertEquals("123", firstField.getText());
        assertEquals("Alice", secondField.getText());
    }

    @Test
    void run_closesDialog_test(FxRobot robot) {
        robot.clickOn("Run");
        waitUntil(robot, () -> !DialogShowing());
        assertFalse(DialogShowing());
    }

    @Test
    void cancel_closesDialog_test(FxRobot robot) {
        robot.clickOn("Cancel");
        waitUntil(robot, () -> !DialogShowing());
        assertFalse(DialogShowing());
    }

    private boolean DialogShowing() {
        return Window.getWindows().stream().anyMatch(w -> w instanceof Stage s && s.isShowing() && "CREATE_EMPLOYEE — parameters".equals(s.getTitle()));
    }

    private void waitUntil(FxRobot robot, Supplier<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {WaitForAsyncUtils.waitForFxEvents();return condition.get();});
        } catch (TimeoutException e) {
            throw new AssertionError();
        }
    }
}

