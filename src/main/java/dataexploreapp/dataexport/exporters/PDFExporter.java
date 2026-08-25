package dataexploreapp.dataexport.exporters;

import dataexploreapp.dataexport.IExporter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import dataexploreapp.db_config.database.DataTable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExporter implements IExporter {

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 20f;
    private static final float FONT_SIZE = 9f;
    private static final float TITLE_FONT_SIZE = 16f;

    // Bundle a font under src/main/resources/fonts/ to get full Unicode/Cyrillic
    // support everywhere. If it's not on the classpath, we fall back to common
    // system font locations, and finally to PDFBox's built-in Helvetica (Latin only,
    // but always available — so export never hard-fails for lack of a font file).
    private static final String REGULAR_FONT_RESOURCE = "/fonts/DejaVuSans.ttf";
    private static final String BOLD_FONT_RESOURCE = "/fonts/DejaVuSans-Bold.ttf";

    private static final String[] REGULAR_FONT_SYSTEM_PATHS = {
            "C:\\Windows\\Fonts\\arial.ttf",                              // Windows
            "/Library/Fonts/Arial.ttf",                                   // macOS
            "/System/Library/Fonts/Supplemental/Arial.ttf",               // macOS
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",            // Linux (Debian/Ubuntu)
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf", // Linux
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",                     // Linux (Fedora)
    };
    private static final String[] BOLD_FONT_SYSTEM_PATHS = {
            "C:\\Windows\\Fonts\\arialbd.ttf",
            "/Library/Fonts/Arial Bold.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
    };

    // True once we've fallen back to the standard 14 font for the current export,
    // so text can be sanitized to characters that font can actually encode.
    private boolean usingStandardFontFallback = false;

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        usingStandardFontFallback = false;
        try (PDDocument document = new PDDocument()) {
            PDFont headerFont = resolveFont(document, BOLD_FONT_RESOURCE, BOLD_FONT_SYSTEM_PATHS, true);
            PDFont bodyFont = resolveFont(document, REGULAR_FONT_RESOURCE, REGULAR_FONT_SYSTEM_PATHS, false);

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

    /**
     * Resolves a usable font in three steps:
     *  1. A bundled classpath resource (works identically on every OS).
     *  2. A known system font path for the current OS.
     *  3. PDFBox's built-in standard 14 font (Helvetica/Helvetica-Bold) — always
     *     available, so export never throws for lack of a font file. Latin-only,
     *     so text gets sanitized (see sanitizeForFont) when this tier is used.
     */
    private PDFont resolveFont(PDDocument document, String classpathResource, String[] systemPaths, boolean bold)
            throws IOException {

        try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
            if (in != null) {
                return PDType0Font.load(document, in);
            }
        }

        for (String path : systemPaths) {
            File fontFile = new File(path);
            if (fontFile.exists()) {
                return PDType0Font.load(document, fontFile);
            }
        }

        usingStandardFontFallback = true;
        return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
    }

    /**
     * When falling back to the standard 14 font, strip characters it can't encode
     * (e.g. Cyrillic) so showText() doesn't throw mid-export. Embedded TrueType
     * fonts (the normal case) pass text through untouched.
     */
    private String sanitizeForFont(PDFont font, String text) {
        if (text == null || !usingStandardFontFallback) {
            return text == null ? "" : text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (IOException | IllegalArgumentException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private float drawTitle(PDPageContentStream cs, DataTable table, PDFont font, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, TITLE_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y - TITLE_FONT_SIZE);
        cs.showText(sanitizeForFont(font, table.getTitle()));
        cs.endText();

        y -= TITLE_FONT_SIZE + 6;

        cs.beginText();
        cs.setFont(font, 8);
        cs.newLineAtOffset(MARGIN, y - 10);
        String subtitle = "Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "  |  " + table.getRowCount() + " rows";
        cs.showText(sanitizeForFont(font, subtitle));
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
            cs.showText(truncate(sanitizeForFont(font, columnNames.get(i)), colWidths[i], font, FONT_SIZE));
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
            cs.showText(truncate(sanitizeForFont(font, text), colWidths[i], font, FONT_SIZE));
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