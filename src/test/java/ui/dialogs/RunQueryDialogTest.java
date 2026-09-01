package ui.dialogs;

import dataexploreapp.dialogs.RunQueryDialog;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RunQueryDialogTest {

    @Start
    private void start(Stage stage) {
        stage.setScene(new Scene(new Pane(), 200, 100));
        stage.show();

        Platform.runLater(() -> RunQueryDialog.show(stage));
    }


    //proveruva dali se zatvora dialog odkako ke runne query
    @Test
    void runningValidQuery_test(FxRobot robot) {
        robot.clickOn("#queryArea");
        robot.write("SELECT * FROM EMPLOYEES");
        robot.clickOn("#runBtn");

        waitUntil(robot, () -> !DialogShowing());

        assertFalse(DialogShowing());
    }

    @Test
    void invalidQuery_error_test(FxRobot robot) {
        robot.clickOn("#queryArea");
        robot.write("DROP TABLE EMPLOYEES");
        robot.clickOn("#runBtn");

        waitUntil(robot, () -> !errorLabel(robot).getText().isEmpty());

        assertEquals("Only SELECT statements and stored procedure calls are allowed.", errorLabel(robot).getText());
        assertTrue(DialogShowing());
    }

    @Test
    void emptyQuery_error_test(FxRobot robot) {
        robot.clickOn("#runBtn");
        waitUntil(robot, () -> !errorLabel(robot).getText().isEmpty());

        assertEquals("SQL query cannot be empty.", errorLabel(robot).getText());
        assertTrue(DialogShowing());
    }


    //proveruva dali cancel go zatvora dialogot
    @Test
    void cancel_test(FxRobot robot) {
        robot.clickOn("#cancelBtn");
        waitUntil(robot, () -> !DialogShowing());
        assertFalse(DialogShowing());
    }
//helpers
    private boolean DialogShowing() {
        return Window.getWindows().stream().anyMatch(w -> w instanceof Stage s && s.isShowing() && "Run query".equals(s.getTitle()));
    }

    private Label errorLabel(FxRobot robot) {
        return robot.lookup("#errorLabel").queryAs(Label.class);
    }

    private void waitUntil(FxRobot robot, Supplier<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> {WaitForAsyncUtils.waitForFxEvents();return condition.get();});
        } catch (TimeoutException e) {
            throw new AssertionError();
        }
    }
}