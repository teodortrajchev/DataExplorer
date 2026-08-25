package dataexploreapp.dataexport.exporters;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataexploreapp.dataexport.IExporter;
import dataexploreapp.db_config.database.DataTable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONExporter implements IExporter {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();
        List<String> columns = table.getColumnNames();

        for (Object[] row : table.getRows()) {
            Map<String, Object> record = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                record.put(columns.get(i), row[i]);
            }
            records.add(record);
        }

        mapper.writeValue(outputFile.toFile(), records);
    }

    @Override
    public String fileExtension() { return "json"; }
}
