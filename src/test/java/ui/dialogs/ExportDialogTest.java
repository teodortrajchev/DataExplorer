package ui.dialogs;

import dataexploreapp.dataexport.ExportFormat;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.dialogs.ExportDialog;
import dataexploreapp.history.ExportHistoryService;
import dataexploreapp.history.ExportRecord;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ExportDialogTest {

    private Stage stage;
    private DataTable dataTable;
    private ExportDialog dialog;

    @Start
    private void start(Stage stage) {

        this.stage = stage;

        dataTable = new DataTable(List.of("ID", "NAME", "EMAIL"));

        dataTable.addRow(new Object[]{1, "Teo", "teo@test.com"});
        dataTable.addRow(new Object[]{2, "John", "johndoe@test.com"});

        ExportHistoryService.clear();
        dialog = new ExportDialog(
                dataTable,
                "USERS",
                false,
                null,
                null,
                null,
                null,
                null
        );
        dialog.show(stage);
    }

    @BeforeEach
    void cleanHistory() {
        ExportHistoryService.clear();
    }

    @Test
    void exportDialog_test(FxRobot robot) {

        assertTrue(stage.isShowing());
        assertEquals("Export data", stage.getTitle());
    }

    @Test
    void rowCount_test(FxRobot robot) {

        Label title = robot.lookup("Export 2 rows").queryAs(Label.class);

        assertEquals("Export 2 rows", title.getText());
    }

    @Test
    void formatComboBox_test(FxRobot robot) {

        ComboBox<?> formatBox = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertNotNull(formatBox);
        assertFalse(formatBox.getItems().isEmpty());
    }

    @Test
    void exportFormats_test(FxRobot robot) {

        ComboBox<?> formatBox = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertEquals(ExportFormat.values().length, formatBox.getItems().size());

        for (ExportFormat format : ExportFormat.values()) {
            assertTrue(formatBox.getItems().contains(format));
        }
    }

    @Test
    void exportFormatDefault_test(FxRobot robot) {

        ComboBox<ExportFormat> formatBox = robot.lookup(".combo-box").queryAs(ComboBox.class);

        assertNotNull(formatBox.getValue());
        assertEquals(ExportFormat.values()[0], formatBox.getValue());
    }

    @Test
    void chooseFileBtn_est(FxRobot robot) {

        Button button = robot.lookup("Choose output file...").queryAs(Button.class);

        assertNotNull(button);
        assertTrue(button.isVisible());
        assertFalse(button.isDisabled());
    }

    @Test
    void exportButton_test(FxRobot robot) {

        Button button = robot.lookup("Export").queryAs(Button.class);

        assertNotNull(button);
        assertTrue(button.isVisible());
        assertFalse(button.isDisabled());
    }

    @Test
    void exportWithoutFile_test(FxRobot robot) {

        robot.clickOn("Export");

        Label status = robot.lookup(".label")
                .queryAllAs(Label.class)
                .stream()
                .filter(label -> "Choose an output file first.".equals(label.getText())).findFirst().orElseThrow(() -> new AssertionError());

        assertEquals("Choose an output file first.", status.getText());
    }

    @Test
    void selectingDifferentFormat_test(FxRobot robot) {

        ComboBox<ExportFormat> formatBox = robot.lookup(".combo-box").queryAs(ComboBox.class);
        ExportFormat[] formats = ExportFormat.values();

        if (formats.length < 2) {
            return;
        }

        robot.clickOn(formatBox);
        robot.clickOn(formats[1].toString());
        assertEquals(formats[1], formatBox.getValue());
    }

    @Test
    void export_createsFile_test(FxRobot robot) throws Exception {

        ExportFormat format = ExportFormat.values()[0];

        Path tempFile = Files.createTempFile("dataexplorer-test-", format.getExtension());

        Files.deleteIfExists(tempFile);

        setSelectedFile(dialog, tempFile.toFile());

        robot.clickOn("Export");

        waitUntil(() -> {
            Label status = findStatusLabel(robot);
            return status != null && status.getText().startsWith("Done:");
        });

        assertTrue(Files.exists(tempFile));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void successfulExportHistory_test(FxRobot robot) throws Exception {

        ExportFormat format = ExportFormat.values()[0];

        Path tempFile = Files.createTempFile("dataexplorer-history-", format.getExtension());

        Files.deleteIfExists(tempFile);

        ExportDialog dialog = this.dialog;

        setSelectedFile(dialog, tempFile.toFile());

        robot.clickOn("Export");

        waitUntil(() -> ExportHistoryService.loadHistory().size() == 1);

        List<ExportRecord> history = ExportHistoryService.loadHistory();

        assertEquals(1, history.size());

        ExportRecord record = history.get(0);

        assertEquals("USERS", record.getTableName());
        assertEquals(format.toString(), record.getFormat());
        assertEquals(2, record.getRowCount());
        assertEquals(tempFile.toFile().getAbsolutePath(), record.getOutputPath());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void successfulExport_status_test(FxRobot robot) throws Exception {

        ExportFormat format = ExportFormat.values()[0];

        Path tempFile = Files.createTempFile("dataexplorer-status-", format.getExtension());

        Files.deleteIfExists(tempFile);

        setSelectedFile(dialog, tempFile.toFile());

        robot.clickOn("Export");

        waitUntil(() -> {
            Label status = findStatusLabel(robot);
            return status != null && status.getText().equals("Done: " + tempFile.getFileName());
        });

        Label status = findStatusLabel(robot);

        assertEquals("Done: " + tempFile.getFileName(), status.getText());

        Files.deleteIfExists(tempFile);
    }

    private Label findStatusLabel(FxRobot robot) {

        return robot.lookup(".status-label")
                .queryAllAs(Label.class)
                .stream()
                .filter(label -> label.getText() != null && (label.getText().startsWith("Done:") || label.getText().startsWith("Export") || label.getText().startsWith("Choose")))
                .findFirst()
                .orElse(null);
    }

    private void waitUntil(java.util.function.Supplier<Boolean> condition) {

        try {

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> {WaitForAsyncUtils.waitForFxEvents();return condition.get();});


        } catch (Exception e) {

            throw new AssertionError("Condition was not met within 10 seconds", e);
        }
    }


    private void setSelectedFile(ExportDialog dialog, File file) throws Exception {

        Field field = ExportDialog.class.getDeclaredField("selectedFile");
        field.setAccessible(true);
        field.set(dialog, file);
    }
}
