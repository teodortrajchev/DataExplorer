package Config.Exporters;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import Config.Database.DataTable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CSVExporter implements IExporter {

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(table.getColumnNames().toArray(new String[0]))
                .build();

        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (Object[] row : table.getRows()) {
                printer.printRecord((Object[]) row);
            }
        }
    }

    @Override
    public String fileExtension() { return "csv"; }
}