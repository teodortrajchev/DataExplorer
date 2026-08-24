package dataexploreapp.dataexport;
import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.dataexport.exporters.ExportFormat;
import dataexploreapp.dataexport.exporters.IExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class DataExportService {
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);
    private final DataBaseReader reader;

    public DataExportService(DataBaseReader reader) {
        this.reader = reader;
    }
    public void export(
            String query,
            ExportFormat format,
            Path output,
            String table_title
    ) {

        logger.info("Starting export: format={}, output={}", format, output.toAbsolutePath());

        DataTable table;

        try {

            logger.debug("Executing query for export");

            table = reader.runQuery(query);

            logger.info("Data retrieved successfully: {} rows, {} columns", table.getRowCount(), table.getColumnCount());

        } catch (SQLException e) {

            logger.error("Failed to retrieve data for export: {}", e.getMessage(), e);

            throw new RuntimeException(e);
        }

        table.setTitle(table_title);

        IExporter exporter = format.getExporter();

        try {

            logger.info("Writing {} rows to {}", table.getRowCount(), output.toAbsolutePath());

            exporter.export(table, output);

            logger.info("Export completed successfully: {}", output.toAbsolutePath());

        } catch (IOException e) {

            logger.error("Failed to write export file: {}", e.getMessage(), e);

            throw new RuntimeException(e);
        }
    }
}
