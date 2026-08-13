package dataexploreapp.importfiles;

import dataexploreapp.db_config.database.DataTable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public class DataImportService {

    public static DataTable importFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        DataTable table;

        if (name.endsWith(".csv")) {
            table = importCsv(file);
        } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            table = importExcel(file);
        } else if (name.endsWith(".docx")) {
            table = importWord(file);
        } else if (name.endsWith(".pdf")) {
            table = importPdf(file);
        } else {
            throw new IOException("Unsupported file type: " + file.getName());
        }

        table.setTitle(file.getName());
        return table;
    }

    private static DataTable importCsv(File file) throws IOException {
        try (Reader reader = new FileReader(file);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<String> columns = new ArrayList<>(parser.getHeaderNames());
            DataTable table = new DataTable(columns);

            for (CSVRecord record : parser) {
                Object[] row = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    row[i] = record.get(i);
                }
                table.addRow(row);
            }
            return table;
        }
    }

    private static DataTable importExcel(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            List<String> columns = new ArrayList<>();
            for (Cell cell : headerRow) {
                columns.add(cell.getStringCellValue());
            }

            DataTable table = new DataTable(columns);
            DataFormatter formatter = new DataFormatter();

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                Object[] rowData = new Object[columns.size()];
                for (int c = 0; c < columns.size(); c++) {
                    Cell cell = row.getCell(c);
                    rowData[c] = cell == null ? "" : formatter.formatCellValue(cell);
                }
                table.addRow(rowData);
            }
            return table;
        }
    }

    /** Reads the first table found in the .docx. */
    private static DataTable importWord(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {

            List<XWPFTable> tables = doc.getTables();
            if (tables.isEmpty()) {
                throw new IOException("No tables found in this Word document.");
            }

            XWPFTable xwpfTable = tables.get(0);
            List<XWPFTableRow> rows = xwpfTable.getRows();
            if (rows.isEmpty()) {
                throw new IOException("The table in this document has no rows.");
            }

            List<String> columns = new ArrayList<>();
            for (XWPFTableCell cell : rows.get(0).getTableCells()) {
                columns.add(cell.getText().trim());
            }

            DataTable table = new DataTable(columns);
            for (int r = 1; r < rows.size(); r++) {
                List<XWPFTableCell> cells = rows.get(r).getTableCells();
                Object[] rowData = new Object[columns.size()];
                for (int c = 0; c < columns.size(); c++) {
                    rowData[c] = c < cells.size() ? cells.get(c).getText().trim() : "";
                }
                table.addRow(rowData);
            }
            return table;
        }
    }

    // ---------------------------------------------------------------------
    // PDF import: position-aware table extraction.
    //
    // PDFBox has no built-in table detection, and plain getText() loses all
    // cell-boundary information — it just linearizes reading order. So this
    // tracks the exact x/y coordinate of every character, groups characters
    // into rows by y-proximity, groups each row's characters into words by
    // x-gap, and assigns each word to a column by comparing its x-position
    // to the header row's column start positions. Repeated headers/captions
    // that appear on every page are detected and skipped.
    //
    // This still isn't a general table parser: cells that visually span
    // multiple columns will land in whichever single column their start
    // position maps to, and irregular/rotated layouts may not extract
    // cleanly. Spot-check results on complex PDFs.
    // ---------------------------------------------------------------------

    private static final float ROW_Y_TOLERANCE = 2.5f;
    private static final float WORD_GAP_THRESHOLD = 3.0f;
    private static final float COLUMN_BOUNDARY_SLACK = 3.0f;

    private record CharPos(int page, float x, float width, float y, String ch) {}
    private record Word(float x, String text) {}
    private record Row(int page, List<Word> words) {
        String joined() {
            return words.stream().map(Word::text).collect(Collectors.joining(" ")).trim();
        }
    }

    private static class PositionalTextStripper extends PDFTextStripper {
        final List<CharPos> chars = new ArrayList<>();

        PositionalTextStripper() throws IOException {
            super();
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            chars.add(new CharPos(getCurrentPageNo(), text.getXDirAdj(), text.getWidthDirAdj(), text.getYDirAdj(), text.getUnicode()));
            super.processTextPosition(text);
        }
    }

    private static DataTable importPdf(File file) throws IOException {
        List<CharPos> chars;
        try (PDDocument document = Loader.loadPDF(file)) {
            PositionalTextStripper stripper = new PositionalTextStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document); // discard string result; we only need the captured positions
            chars = stripper.chars;
        }

        if (chars.isEmpty()) {
            throw new IOException("No extractable text found in this PDF.");
        }

        Map<Integer, List<CharPos>> byPage = new TreeMap<>();
        for (CharPos c : chars) {
            byPage.computeIfAbsent(c.page(), k -> new ArrayList<>()).add(c);
        }

        List<Row> allRows = new ArrayList<>();
        for (Map.Entry<Integer, List<CharPos>> entry : byPage.entrySet()) {
            allRows.addAll(groupIntoRows(entry.getKey(), entry.getValue()));
        }
        if (allRows.isEmpty()) {
            throw new IOException("No extractable table data found in this PDF.");
        }

        int firstPage = byPage.keySet().iterator().next();
        List<Row> page1Rows = allRows.stream().filter(r -> r.page() == firstPage).toList();

        // The real header usually has several widely-spaced column labels; a
        // stray caption/date line above it is normally just one run of prose.
        // So among the first few rows of page 1, pick the one with the most
        // distinct word clusters.
        int headerScanLimit = Math.min(6, page1Rows.size());
        Row headerRow = page1Rows.get(0);
        for (int i = 0; i < headerScanLimit; i++) {
            if (page1Rows.get(i).words().size() > headerRow.words().size()) {
                headerRow = page1Rows.get(i);
            }
        }
        if (headerRow.words().size() < 2) {
            throw new IOException("Could not detect a table header in this PDF — the layout may be too irregular for automatic extraction.");
        }

        List<Float> columnBoundaries = headerRow.words().stream().map(Word::x).sorted().toList();
        List<String> columnNames = headerRow.words().stream().map(w -> w.text().trim()).toList();

        // Anything before the header on page 1 (e.g. a stray caption), plus the
        // header text itself, gets skipped wherever it repeats on later pages.
        Set<String> boilerplate = new HashSet<>();
        boilerplate.add(headerRow.joined().toLowerCase());
        for (Row r : page1Rows) {
            if (r == headerRow) break;
            boilerplate.add(r.joined().toLowerCase());
        }

        DataTable table = new DataTable(new ArrayList<>(columnNames));
        boolean pastHeader = false;

        for (Row row : allRows) {
            if (row == headerRow) {
                pastHeader = true;
                continue;
            }
            if (!pastHeader) continue;
            if (row.words().isEmpty()) continue;
            if (boilerplate.contains(row.joined().toLowerCase())) continue;

            Object[] rowData = new Object[columnNames.size()];
            Arrays.fill(rowData, "");
            for (Word word : row.words()) {
                int col = assignColumn(word.x(), columnBoundaries);
                String existing = (String) rowData[col];
                rowData[col] = existing.isEmpty() ? word.text() : existing + " " + word.text();
            }
            table.addRow(rowData);
        }

        return table;
    }

    private static int assignColumn(float x, List<Float> boundaries) {
        int col = 0;
        for (int i = 0; i < boundaries.size(); i++) {
            if (x >= boundaries.get(i) - COLUMN_BOUNDARY_SLACK) {
                col = i;
            }
        }
        return col;
    }

    private static List<Row> groupIntoRows(int page, List<CharPos> pageChars) {
        List<CharPos> sortedByY = new ArrayList<>(pageChars);
        sortedByY.sort(Comparator.comparing(CharPos::y));

        List<List<CharPos>> rawRows = new ArrayList<>();
        List<CharPos> currentRow = new ArrayList<>();
        float currentY = Float.NaN;

        for (CharPos c : sortedByY) {
            if (currentRow.isEmpty() || Math.abs(c.y() - currentY) <= ROW_Y_TOLERANCE) {
                currentRow.add(c);
                float sum = 0;
                for (CharPos cp : currentRow) sum += cp.y();
                currentY = sum / currentRow.size();
            } else {
                rawRows.add(currentRow);
                currentRow = new ArrayList<>();
                currentRow.add(c);
                currentY = c.y();
            }
        }
        if (!currentRow.isEmpty()) {
            rawRows.add(currentRow);
        }

        List<Row> rows = new ArrayList<>();
        for (List<CharPos> rawRow : rawRows) {
            rawRow.sort(Comparator.comparing(CharPos::x));
            List<Word> words = groupIntoWords(rawRow);
            if (!words.isEmpty()) {
                rows.add(new Row(page, words));
            }
        }
        return rows;
    }

    private static List<Word> groupIntoWords(List<CharPos> rowChars) {
        List<Word> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        float wordStartX = 0;
        float prevEndX = Float.NaN;

        for (CharPos c : rowChars) {
            boolean newWord = Float.isNaN(prevEndX) || (c.x() - prevEndX) > WORD_GAP_THRESHOLD;
            if (newWord) {
                if (current.length() > 0) {
                    words.add(new Word(wordStartX, current.toString().trim()));
                    current.setLength(0);
                }
                wordStartX = c.x();
            }
            current.append(c.ch());
            prevEndX = c.x() + c.width();
        }
        if (current.length() > 0) {
            words.add(new Word(wordStartX, current.toString().trim()));
        }

        return words.stream().filter(w -> !w.text().isEmpty()).toList();
    }
}