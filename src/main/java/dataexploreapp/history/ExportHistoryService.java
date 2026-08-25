package dataexploreapp.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ExportHistoryService {

    private static final Path HISTORY_FILE = Path.of(System.getProperty("user.home"), ".dataexplorer", "export_history.json");

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static List<ExportRecord> loadHistory() {
        List<ExportRecord> list = readRaw();
        Collections.reverse(list);
        return list;
    }

    public static void append(ExportRecord record) {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            List<ExportRecord> current = readRaw();
            current.add(record);
            mapper.writeValue(HISTORY_FILE.toFile(), current);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<ExportRecord> readRaw() {
        try {
            File file = HISTORY_FILE.toFile();
            if (!file.exists()) {
                return new ArrayList<>();
            }
            ExportRecord[] records = mapper.readValue(file, ExportRecord[].class);
            return new ArrayList<>(List.of(records));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static void clear() {
        try {
            Files.deleteIfExists(HISTORY_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}