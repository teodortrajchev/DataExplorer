package ui;

import dataexploreapp.auth.AuthService;
import dataexploreapp.db_config.save.SavedConnectionService;
import dataexploreapp.dialogs.LoginDialog;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(ApplicationExtension.class)
class LoginDialogTest {

    private AuthService authService;
    private Stage stage;

    @TempDir
    Path tempDir;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        this.authService = mock(AuthService.class);
        new LoginDialog(authService).show(stage);
    }

    @BeforeEach
    void set_temp_conn_file() {
        SavedConnectionService.setConnectionsFile(tempDir.resolve("connections.json"));
    }

    @BeforeEach
    void bringToFront(FxRobot robot) {
        robot.interact(() -> stage.toFront());
    }

    @Test
    void wrongPassword_msg_test(FxRobot robot) {
        when(authService.authenticate("admin", "wrong")).thenReturn(AuthService.AuthResult.WRONG_PASSWORD);

        robot.clickOn("#userIdField").write("admin");
        robot.clickOn("#passwordField").write("wrong");
        robot.clickOn("#loginButton");

        waitUntil(() -> !"Checking...".equals(messageLabel(robot).getText()));

        assertEquals("Login Failed", messageLabel(robot).getText());
        assertFalse(loginButton(robot).isDisable(), "Login button should be re-enabled after the attempt");
    }

    @Test
    void unknownUser_msg_test(FxRobot robot) {
        when(authService.authenticate(anyString(), anyString())).thenReturn(AuthService.AuthResult.USER_NOT_FOUND);

        robot.clickOn("#userIdField").write("ghost");
        robot.clickOn("#passwordField").write("whatever");
        robot.clickOn("#loginButton");

        waitUntil(() -> !"Checking...".equals(messageLabel(robot).getText()));

        assertEquals("Username not found", messageLabel(robot).getText());
    }

    @Test
    void dbError_msg_test(FxRobot robot) {
        when(authService.authenticate(anyString(), anyString())).thenReturn(AuthService.AuthResult.DB_ERROR);

        robot.clickOn("#userIdField").write("admin");
        robot.clickOn("#passwordField").write("admin");
        robot.clickOn("#loginButton");

        waitUntil(() -> !"Checking...".equals(messageLabel(robot).getText()));

        assertEquals("Database error", messageLabel(robot).getText());
    }

    @Test
    void pressingEnter_test(FxRobot robot) {
        when(authService.authenticate("admin", "admin")).thenReturn(AuthService.AuthResult.SUCCESS);

        robot.clickOn("#userIdField").write("admin");
        robot.clickOn("#passwordField").write("admin").type(KeyCode.ENTER);

        waitUntil(() -> "Connect to a database".equals(stage.getTitle()));

        assertEquals("Connect to a database", stage.getTitle());
    }

    @Test
    void successfulLogin_test(FxRobot robot) {
        when(authService.authenticate("admin", "admin")).thenReturn(AuthService.AuthResult.SUCCESS);

        robot.clickOn("#userIdField").write("admin");
        robot.clickOn("#passwordField").write("admin");
        robot.clickOn("#loginButton");

        waitUntil(() -> "Connect to a database".equals(stage.getTitle()));

        assertEquals("Connect to a database", stage.getTitle());
    }

    @Test
    void resetButton_test(FxRobot robot) {
        robot.clickOn("#userIdField").write("someone");
        robot.clickOn("#passwordField").write("secret");
        robot.clickOn("#resetButton");

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("", userIdField(robot).getText());
        assertEquals("", passwordField(robot).getText());
        assertEquals("", messageLabel(robot).getText());
    }

    private TextField userIdField(FxRobot robot) {
        return robot.lookup("#userIdField").queryAs(TextField.class);
    }

    private PasswordField passwordField(FxRobot robot) {
        return robot.lookup("#passwordField").queryAs(PasswordField.class);
    }

    private Label messageLabel(FxRobot robot) {
        return robot.lookup("#messageLabel").queryAs(Label.class);
    }

    private Button loginButton(FxRobot robot) {
        return robot.lookup("#loginButton").queryAs(Button.class);
    }

    // helper
    private void waitUntil(Supplier<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> {
                WaitForAsyncUtils.waitForFxEvents();
                return condition.get();
            });
        } catch (TimeoutException e) {
            throw new AssertionError("Condition was not met within 5 seconds", e);
        }
    }
}