package dataexploreapp.dataexport.exporters;

import dataexploreapp.dataexport.IExporter;
import org.apache.poi.xwpf.usermodel.*;
import dataexploreapp.db_config.database.DataTable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class WordExporter implements IExporter {

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {

            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(table.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            XWPFParagraph subPara = document.createParagraph();
            XWPFRun subRun = subPara.createRun();
            subRun.setItalic(true);
            subRun.setFontSize(10);
            subRun.setText("Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    + " • " + table.getRowCount() + " rows");

            document.createParagraph(); // spacer

            int cols = table.getColumnCount();
            int rows = table.getRowCount() + 1; // +1 for header

            XWPFTable docTable = document.createTable(rows, cols);
            docTable.setWidth("100%");

            XWPFTableRow headerRow = docTable.getRow(0);
            for (int col = 0; col < cols; col++) {
                XWPFTableCell cell = headerRow.getCell(col);
                cell.removeParagraph(0);
                XWPFParagraph p = cell.addParagraph();
                XWPFRun r = p.createRun();
                r.setBold(true);
                r.setText(table.getColumnNames().get(col));
                cell.setColor("D9E2F3");
            }

            int rowIndex = 1;
            for (Object[] rowData : table.getRows()) {
                XWPFTableRow docRow = docTable.getRow(rowIndex);
                for (int col = 0; col < cols; col++) {
                    Object value = rowData[col];
                    docRow.getCell(col).setText(value == null ? "" : value.toString());
                }
                rowIndex++;
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                document.write(fos);
            }
        }
    }

    @Override
    public String fileExtension() { return "docx"; }
}