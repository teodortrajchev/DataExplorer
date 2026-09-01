package ui.dialogs;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.dialogs.UpdateConnectionDialog;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class UpdateConnectionDialogTest {

    private static final String username = "testuser";

    private Stage ownerStage;

    @Start
    private void start(Stage stage) {
        this.ownerStage = stage;
        stage.setScene(new Scene(new Pane(), 200, 100));
        stage.show();
    }

    @Test
    void prefill_dbdata_test(FxRobot robot) {
        DataBaseReader currentReader = mock(DataBaseReader.class);
        when(currentReader.getJdbcUrl()).thenReturn("jdbc:oracle:thin:@//old-host:1521/orclpdb");
        when(currentReader.getUsername()).thenReturn("olduser");

        openDialog(robot, currentReader, "OLDSCHEMA");

        assertEquals("jdbc:oracle:thin:@//old-host:1521/orclpdb", urlField(robot).getText());
        assertEquals("olduser", dbUserField(robot).getText());
        assertEquals("", dbPasswordField(robot).getText(), "password should never be prefilled");
        assertEquals("OLDSCHEMA", schemaField(robot).getText());

        closeDialog(robot);
    }

    @Test
    void missingUrlOrUsername_test(FxRobot robot) {
        DataBaseReader currentReader = mock(DataBaseReader.class);
        when(currentReader.getJdbcUrl()).thenReturn("jdbc:oracle:thin:@//old-host:1521/orclpdb");
        when(currentReader.getUsername()).thenReturn("olduser");

        openDialog(robot, currentReader, "OLDSCHEMA");

        robot.interact(() -> urlField(robot).clear());
        robot.clickOn("#connectButton");

        assertEquals("JDBC URL and username are required.", statusLabel(robot).getText());

        closeDialog(robot);
    }

    @Test
    void cancel_test(FxRobot robot) {
        DataBaseReader currentReader = mock(DataBaseReader.class);
        when(currentReader.getJdbcUrl()).thenReturn("jdbc:oracle:thin:@//old-host:1521/orclpdb");
        when(currentReader.getUsername()).thenReturn("olduser");

        openDialog(robot, currentReader, "OLDSCHEMA");
        assertTrue(DialogShowing());

        robot.clickOn("#cancelButton");
        assertFalse(DialogShowing());
    }

    @Test
    void connectionFail_test(FxRobot robot) {
        DataBaseReader currentReader = mock(DataBaseReader.class);
        when(currentReader.getJdbcUrl()).thenReturn("jdbc:oracle:thin:@//old-host:1521/orclpdb");
        when(currentReader.getUsername()).thenReturn("olduser");

        openDialog(robot, currentReader, "OLDSCHEMA");

        robot.interact(() -> urlField(robot).clear());
        robot.clickOn("#urlField").write("jdbc:oracle:thin:@//localhost:1/nope");
        robot.interact(() -> dbUserField(robot).clear());
        robot.clickOn("#dbUserField").write("someuser");
        robot.clickOn("#connectButton");

        waitUntil(robot, 8, () -> statusLabel(robot).getText().startsWith("Connection failed:"));

        assertTrue(statusLabel(robot).getText().startsWith("Connection failed:"));
        assertFalse(connectButton(robot).isDisable(), "Connect button should be re-enabled after the attempt");

        closeDialog(robot);
    }


    private void openDialog(FxRobot robot, DataBaseReader currentReader, String schema) {
        Platform.runLater(() -> new UpdateConnectionDialog(username, currentReader, schema).show(ownerStage));
        waitUntil(robot, 5, this::DialogShowing);
    }

    private void closeDialog(FxRobot robot) {
        if (DialogShowing()) {
            robot.clickOn("#cancelButton");
        }
    }

    private boolean DialogShowing() {
        return Window.getWindows().stream().anyMatch(w -> w instanceof Stage s && s.isShowing() && "Update Connection".equals(s.getTitle()));
    }


    private TextField urlField(FxRobot robot) {
        return robot.lookup("#urlField").queryAs(TextField.class);
    }

    private TextField dbUserField(FxRobot robot) {
        return robot.lookup("#dbUserField").queryAs(TextField.class);
    }

    private PasswordField dbPasswordField(FxRobot robot) {
        return robot.lookup("#dbPasswordField").queryAs(PasswordField.class);
    }

    private TextField schemaField(FxRobot robot) {
        return robot.lookup("#schemaField").queryAs(TextField.class);
    }

    private Label statusLabel(FxRobot robot) {
        return robot.lookup("#statusLabel").queryAs(Label.class);
    }

    private Button connectButton(FxRobot robot) {
        return robot.lookup("#connectButton").queryAs(Button.class);
    }


    private void waitUntil(FxRobot robot, long timeoutSeconds, Supplier<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(timeoutSeconds, TimeUnit.SECONDS, () -> {
                WaitForAsyncUtils.waitForFxEvents();
                return condition.get();
            });
        } catch (TimeoutException e) {
            throw new AssertionError("Condition was not met within " + timeoutSeconds + " seconds", e);
        }
    }
}