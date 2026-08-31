package ui.pages;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.pages.AccountPage;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;

import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AccountPageTest {

    private Stage stage;
    private DataBaseReader reader;

    @Start
    private void start(Stage stage) throws SQLException {
        this.stage = stage;
        reader = Mockito.mock(DataBaseReader.class);

        DataTable accountData = new DataTable(List.of("created_at"));
        accountData.addRow(new Object[]{"2026-08-27 10:30:00"});

        when(reader.runQuery(anyString())).thenReturn(accountData);

        new AccountPage("testuser", reader, "test").show(stage);
    }

    @BeforeEach
    void bringToFront(FxRobot robot) {
        robot.interact(() -> stage.toFront());
    }

    @Test
    void accountPage_opensSuccessfully_test(FxRobot robot) {

        assertTrue(stage.isShowing());

        assertEquals("Settings", stage.getTitle());
    }

    @Test
    void username_isDisplayed_test(FxRobot robot) {

        Label username = robot.lookup(".welcome-title").queryAs(Label.class);

        assertEquals("testuser", username.getText());
    }

    @Test
    void accountCreatedDate_isDisplayed_test(FxRobot robot) {

        Label createdDate = robot.lookup(".welcome-title").queryAs(Label.class);

        assertTrue(
                robot.lookup(".label")
                        .queryAllAs(Label.class)
                        .stream()
                        .anyMatch(label -> "2026-08-27 10:30:00".equals(label.getText())));
    }

    @Test
    void updateConnectionButton_isDisplayed_test(FxRobot robot) {

        Button updateButton = robot.lookup(".primary-button").queryAs(Button.class);

        assertEquals("Update Connection", updateButton.getText());
        assertFalse(updateButton.isDisabled());
    }

    @Test
    void homeButton_isDisplayed_test(FxRobot robot) {

        Button homeButton = robot.lookup("Home").queryAs(Button.class);

        assertEquals("Home", homeButton.getText());
        assertFalse(homeButton.isDisabled());
    }

    @Test
    void logoutButton_isDisplayed_test(FxRobot robot) {

        Button logoutButton = robot.lookup("Logout").queryAs(Button.class);

        assertEquals("Logout", logoutButton.getText());
        assertFalse(logoutButton.isDisabled());
    }

    @Test
    void logout_closesAccountPageAndOpensLogin_test(FxRobot robot) {

        robot.clickOn("Logout");

        waitUntil(() -> !stage.isShowing());

        assertFalse(stage.isShowing(), "AccountPage stage should close after logout");

        boolean loginWindowOpen = Window.getWindows()
                .stream()
                .filter(Window::isShowing)
                .anyMatch(window -> window instanceof Stage && "Login".equals(((Stage) window).getTitle()));

        assertTrue(loginWindowOpen);
    }

    @Test
    void accountQuery_isExecuted_test(FxRobot robot) {

        try {
            Mockito.verify(reader).runQuery(anyString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void accountQuery_containsUsername_test(FxRobot robot) throws SQLException {

        Mockito.verify(reader).runQuery(Mockito.contains("testuser"));
    }

    private void waitUntil(java.util.function.Supplier<Boolean> condition) {

        try {
            WaitForAsyncUtils.waitFor(10, java.util.concurrent.TimeUnit.SECONDS, () -> {WaitForAsyncUtils.waitForFxEvents();return condition.get();});
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("Condition was not met within 10 seconds", e);
        }
    }
}

