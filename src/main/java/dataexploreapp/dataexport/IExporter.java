package dataexploreapp.dataexport;

import dataexploreapp.db_config.database.DataTable;

import java.io.IOException;
import java.nio.file.Path;

public interface IExporter {
    void export(DataTable table, Path outputFile) throws IOException;
    String fileExtension();
}
