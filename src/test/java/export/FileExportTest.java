package export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dataexploreapp.dataexport.DataExportService;
import dataexploreapp.dataexport.ExportFormat;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileExportTest {

    @Mock
    private DataBaseReader reader;
    @TempDir
    Path tempDir;
    private DataExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new DataExportService(reader);
    }

    private DataTable initial_table() {
        DataTable table = new DataTable(List.of("id", "name", "salary"));
        table.addRow(new Object[]{1, "Teo", 100});
        table.addRow(new Object[]{2, "John", 250});
        return table;
    }

    @Test
    void export_query_csv_test() throws Exception {
        when(reader.runQuery(anyString())).thenReturn(initial_table());
        Path output = tempDir.resolve("export.csv");
        exportService.export("SELECT * FROM users", ExportFormat.CSV, output, "Users");

        verify(reader, times(1)).runQuery("SELECT * FROM users");
        assertTrue(Files.exists(output));

        try (Reader fileReader = new FileReader(output.toFile());
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().build().parse(fileReader)) {

            List<CSVRecord> records = parser.getRecords();
            assertEquals(2, records.size());
            assertEquals("Teo", records.get(0).get("name"));
            assertEquals("250", records.get(1).get("salary"));
        }
    }

    @Test
    void export_title_test() throws SQLException {
        DataTable table = initial_table();
        when(reader.runQuery(anyString())).thenReturn(table);

        Path output = tempDir.resolve("titled.json");
        exportService.export("SELECT * FROM users", ExportFormat.JSON, output, "Q3 Users");

        assertEquals("Q3 Users", table.getTitle());
        assertTrue(Files.exists(output));
    }

//  Ne exportira files koga se javuva greska vo readerot
    @Test
    void reader_write_exception_test() throws SQLException {
        when(reader.runQuery(anyString())).thenThrow(new SQLException());
        Path output = tempDir.resolve("never-written.json");
        assertThrows(RuntimeException.class, () -> exportService.export("SELECT 1", ExportFormat.JSON, output, "Title"));

        verify(reader, times(1)).runQuery("SELECT 1");
        assertFalse(Files.exists(output));
    }



    private void valid_csv_test(Path output) throws IOException {
        try (Reader fileReader = new FileReader(output.toFile());
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().build().parse(fileReader)) {
            List<CSVRecord> records = parser.getRecords();
            assertEquals(2, records.size());
            assertEquals("Teo", records.get(0).get("name"));
        }
    }

    private void valid_json_test(Path output) throws IOException {
        JsonNode root = new ObjectMapper().readTree(output.toFile());
        assertTrue(root.isArray());
        assertEquals(2, root.size());
        assertEquals("Teo", root.get(0).get("name").asText());
    }

    private void valid_xlsx_test(Path output) throws IOException {
        try (FileInputStream fis = new FileInputStream(output.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("id", header.getCell(0).getStringCellValue());
            assertEquals("name", header.getCell(1).getStringCellValue());
            // header + 2 data rows
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("Teo", sheet.getRow(1).getCell(1).getStringCellValue());
        }
    }

    private void valid_xml_test(Path output) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(output.toFile());
        assertEquals("data", doc.getDocumentElement().getTagName());
        assertEquals("Users", doc.getDocumentElement().getAttribute("title"));
        NodeList rows = doc.getElementsByTagName("row");
        assertEquals(2, rows.getLength());
    }

    private void valid_docx_test(Path output) throws IOException {
        try (FileInputStream fis = new FileInputStream(output.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {
            assertFalse(document.getParagraphs().isEmpty());
            XWPFTable table = document.getTables().get(0);
            // header + 2 data rows
            assertEquals(3, table.getRows().size());
            assertEquals("id", table.getRow(0).getCell(0).getText());
            assertEquals("Teo", table.getRow(1).getCell(1).getText());
        }
    }

    private void valid_pdf_test(Path output) throws IOException {
        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Users"));
            assertTrue(text.contains("John"));
        }
    }


    @ParameterizedTest
    @EnumSource(ExportFormat.class)
    void export_format_tests(ExportFormat format) throws SQLException, IOException {
        when(reader.runQuery(anyString())).thenReturn(initial_table());
        Path output = tempDir.resolve("export" + format.getExtension());

        exportService.export("SELECT * FROM users", format, output, "Users");
        assertTrue(Files.exists(output) );
        assertTrue(Files.size(output) > 0);

        switch (format) {
            case CSV -> valid_csv_test(output);
            case JSON -> valid_json_test(output);
            case XLSX -> valid_xlsx_test(output);
            case XML -> {
                try {
                    valid_xml_test(output);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case ODX -> valid_docx_test(output);
            case PDF -> valid_pdf_test(output);
        }
    }
}