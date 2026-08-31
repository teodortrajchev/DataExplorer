package ui.pages;

import dataexploreapp.pages.ConnectionPage;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
class ConnectionPageTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        new ConnectionPage("testuser").show(stage);
    }

    @Test
    void missingUrlAndUsername_msg_test(FxRobot robot) {
        robot.clickOn("#connectButton");

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("JDBC URL and username are required.", statusLabel(robot).getText());
    }

    @Test
    void missingUsernameOnly_test(FxRobot robot) {
        robot.clickOn("#urlField").write("jdbc:oracle:thin:@//localhost:1521/orclpdb");
        robot.clickOn("#connectButton");

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("JDBC URL and username are required.", statusLabel(robot).getText());
    }

    @Test
    void saveConnectionCheckbox_test(FxRobot robot) {
        TextField connectionNameField = robot.lookup("#connectionNameField").queryAs(TextField.class);
        CheckBox defaultConnectionCheckBox = robot.lookup("#defaultConnectionCheckBox").queryAs(CheckBox.class);

        // Disabled until "save this connection" is checked.
        assertTrue(connectionNameField.isDisable());
        assertTrue(defaultConnectionCheckBox.isDisable());

        robot.clickOn("#saveConnectionCheckBox");

        assertFalse(connectionNameField.isDisable());
        assertFalse(defaultConnectionCheckBox.isDisable());

        robot.clickOn("#defaultConnectionCheckBox");
        assertTrue(defaultConnectionCheckBox.isSelected());

        // Unchecking "save this connection" disables the fields again and clears "use as default", since a default only makes sense for a saved connection.
        robot.clickOn("#saveConnectionCheckBox");

        assertTrue(connectionNameField.isDisable());
        assertTrue(defaultConnectionCheckBox.isDisable());
        assertFalse(defaultConnectionCheckBox.isSelected());
    }

    @Test
    void connectionFailed_msg_test(FxRobot robot) {
        robot.clickOn("#urlField").write("jdbc:oracle:thin:@//localhost:1/nope");
        robot.clickOn("#dbUserField").write("someuser");
        robot.clickOn("#connectButton");

        waitUntil(robot, 8, () -> statusLabel(robot).getText().startsWith("Connection failed:"));

        assertTrue(statusLabel(robot).getText().startsWith("Connection failed:"));
        assertFalse(connectButton(robot).isDisable(), "Connect button should be re-enabled after the attempt");
    }

    @Test
    void logout_test(FxRobot robot) {

        robot.clickOn("#logoutBtn");

        waitUntil(robot, 5, () -> !stage.isShowing());

        assertFalse(stage.isShowing(), "ConnectionPage's stage should close on logout");

        boolean loginWindowOpen = Window.getWindows().stream()
                .filter(Window::isShowing)
                .anyMatch(w -> w instanceof Stage s && "Login".equals(s.getTitle()));

        assertTrue(loginWindowOpen, "Logout should open a new Login window");
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