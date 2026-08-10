package DataExportApp.DataExport;
import Config.Database.DataBaseReader;
import Config.Database.DataTable;
import Config.Exporters.ExportFormat;
import Config.Exporters.IExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class DataExportService {

    public void export(String db_url, String db_user, String db_pass , String query, ExportFormat format, Path output, String table_title){
        DataBaseReader reader = new DataBaseReader(db_url, db_user, db_pass);

        DataTable table = null;
        try {
            table = reader.runQuery(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        table.setTitle(table_title);

        IExporter exporter = format.getExporter();

        try {
            exporter.export(table, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
