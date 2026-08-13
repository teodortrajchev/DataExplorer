package dataexploreapp.dataexport.exporters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import dataexploreapp.db_config.database.DataTable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExporter implements IExporter {

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 20f;
    private static final float FONT_SIZE = 9f;
    private static final float TITLE_FONT_SIZE = 16f;

    // Windows system fonts — support Cyrillic, unlike PDFBox's built-in Helvetica
    private static final String REGULAR_FONT_PATH = "C:\\Windows\\Fonts\\arial.ttf";
    private static final String BOLD_FONT_PATH = "C:\\Windows\\Fonts\\arialbd.ttf";

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont headerFont = loadFont(document, BOLD_FONT_PATH);
            PDFont bodyFont = loadFont(document, REGULAR_FONT_PATH);

            PDRectangle pageSize = PDRectangle.A4;
            if (table.getColumnCount() > 5) {
                pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            }

            float usableWidth = pageSize.getWidth() - 2 * MARGIN;
            float[] colWidths = computeColumnWidths(table, usableWidth);

            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);

            float y = pageSize.getHeight() - MARGIN;
            y = drawTitle(cs, table, headerFont, y);
            y = drawHeaderRow(cs, table, headerFont, colWidths, y);

            for (Object[] rowData : table.getRows()) {
                if (y < MARGIN + ROW_HEIGHT) {
                    cs.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    y = pageSize.getHeight() - MARGIN;
                    y = drawHeaderRow(cs, table, headerFont, colWidths, y);
                }
                drawDataRow(cs, rowData, bodyFont, colWidths, y);
                y -= ROW_HEIGHT;
            }

            cs.close();
            document.save(outputFile.toFile());
        }
    }

    private PDFont loadFont(PDDocument document, String path) throws IOException {
        File fontFile = new File(path);
        if (!fontFile.exists()) {
            throw new IOException("Font file not found: " + path
                    + " — update REGULAR_FONT_PATH/BOLD_FONT_PATH in PDFExporter to a valid .ttf on this machine.");
        }
        return PDType0Font.load(document, fontFile);
    }

    private float drawTitle(PDPageContentStream cs, DataTable table, PDFont font, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y - TITLE_FONT_SIZE);
        cs.showText(table.getTitle());
        cs.endText();

        y -= TITLE_FONT_SIZE + 6;

        cs.beginText();
        cs.setFont(font, 8);
        cs.newLineAtOffset(MARGIN, y - 10);
        String subtitle = "Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "  |  " + table.getRowCount() + " rows";
        cs.showText(subtitle);
        cs.endText();

        return y - 24;
    }

    private float drawHeaderRow(PDPageContentStream cs, DataTable table, PDFont font, float[] colWidths, float y)
            throws IOException {
        cs.setNonStrokingColor(0.85f, 0.85f, 0.9f);
        cs.addRect(MARGIN, y - ROW_HEIGHT + 4, sum(colWidths), ROW_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);

        float x = MARGIN;
        List<String> columnNames = table.getColumnNames();
        for (int i = 0; i < columnNames.size(); i++) {
            cs.beginText();
            cs.setFont(font, FONT_SIZE);
            cs.newLineAtOffset(x + 3, y - ROW_HEIGHT + 8);
            cs.showText(truncate(columnNames.get(i), colWidths[i], font, FONT_SIZE));
            cs.endText();
            x += colWidths[i];
        }
        return y - ROW_HEIGHT;
    }

    private void drawDataRow(PDPageContentStream cs, Object[] rowData, PDFont font, float[] colWidths, float y)
            throws IOException {
        float x = MARGIN;
        for (int i = 0; i < rowData.length; i++) {
            String text = rowData[i] == null ? "" : rowData[i].toString();
            cs.beginText();
            cs.setFont(font, FONT_SIZE);
            cs.newLineAtOffset(x + 3, y - ROW_HEIGHT + 8);
            cs.showText(truncate(text, colWidths[i], font, FONT_SIZE));
            cs.endText();
            x += colWidths[i];
        }
    }

    private String truncate(String text, float maxWidth, PDFont font, float fontSize) throws IOException {
        if (text == null) return "";
        String result = text;
        while (result.length() > 0 && font.getStringWidth(result) / 1000 * fontSize > maxWidth - 6) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.equals(text) && result.length() > 3) {
            result = result.substring(0, result.length() - 3) + "...";
        }
        return result;
    }

    private float[] computeColumnWidths(DataTable table, float usableWidth) {
        int cols = table.getColumnCount();
        float[] widths = new float[cols];
        float equalWidth = usableWidth / cols;
        for (int i = 0; i < cols; i++) {
            widths[i] = equalWidth;
        }
        return widths;
    }

    private float sum(float[] values) {
        float total = 0;
        for (float v : values) total += v;
        return total;
    }

    @Override
    public String fileExtension() { return "pdf"; }
}