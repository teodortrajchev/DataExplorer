package dataexploreapp.dataexport.exporters;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import dataexploreapp.db_config.database.DataTable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Exports a DataTable to a .xlsx workbook with a styled header row,
 * auto-sized columns, and light zebra striping.
 */
public class ExcelExporter implements IExporter {

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(safeSheetName(table.getTitle()));

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle stripeStyle = buildStripeStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < table.getColumnCount(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(table.getColumnNames().get(col));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Object[] rowData : table.getRows()) {
                Row row = sheet.createRow(rowIndex);
                for (int col = 0; col < rowData.length; col++) {
                    Cell cell = row.createCell(col);
                    setCellValue(cell, rowData[col]);
                    if (rowIndex % 2 == 0) {
                        cell.setCellStyle(stripeStyle);
                    }
                }
                rowIndex++;
            }

            sheet.createFreezePane(0, 1);
            for (int col = 0; col < table.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                workbook.write(fos);
            }
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof java.sql.Date date) {
            cell.setCellValue(date);
        } else if (value instanceof java.sql.Timestamp ts) {
            cell.setCellValue(new java.util.Date(ts.getTime()));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle buildStripeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String safeSheetName(String title) {
        String cleaned = title.replaceAll("[\\\\/?*\\[\\]]", "-");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    @Override
    public String fileExtension() { return "xlsx"; }
}