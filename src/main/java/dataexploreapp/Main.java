package dataexploreapp;

import dataexploreapp.db_config.database.DataBaseReader;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.dataexport.exporters.ExportFormat;
import dataexploreapp.dataexport.exporters.IExporter;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Command-line entry point.
 *
 * Example (Oracle):
 *   java -jar db-exporter.jar \
 *     --url "jdbc:oracle:thin:@//myhost:1521/ORCLPDB1" \
 *     --user myuser --password mypass \
 *     --query "SELECT * FROM employees" \
 *     --format xlsx --output employees.xlsx
 */
@Command(name = "db-exporter", mixinStandardHelpOptions = true, version = "db-exporter 1.0.0",
        description = "Runs a SQL query and exports the results to Excel, Word, PDF, CSV, JSON, or XML.")
public class Main implements Callable<Integer> {
    private static final Logger logger =
            LoggerFactory.getLogger(Main.class);
    @Option(names = {"-u", "--url"}, required = true,
            description = "JDBC URL, e.g. jdbc:oracle:thin:@//host:1521/service_name")
    private String jdbcUrl;

    @Option(names = {"--user"}, defaultValue = "", description = "Database username")
    private String username;

    @Option(names = {"--password"}, defaultValue = "", description = "Database password")
    private String password;

    @Option(names = {"-q", "--query"}, required = true, description = "SQL SELECT statement to run")
    private String query;

    @Option(names = {"-f", "--format"}, required = true,
            description = "Output format: ${COMPLETION-CANDIDATES}")
    private ExportFormat format;

    @Option(names = {"-o", "--output"}, required = true, description = "Output file path")
    private Path output;

    @Option(names = {"-t", "--title"}, defaultValue = "Exported Data", description = "Title shown in the exported document")
    private String title;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            logger.info("Starting DataExporter");

            Dotenv dotenv = Dotenv.load();

            String DB_URL = dotenv.get("DB_URL");
            String DB_USER = dotenv.get("DB_USER");
            String DB_PASS = dotenv.get("DB_PASS");

            logger.info("Creating database reader for user: {}", DB_USER);

            DataBaseReader reader = new DataBaseReader(DB_URL, DB_USER, DB_PASS);

            logger.info("Executing SQL query");

            DataTable table = reader.runQuery(query);

            table.setTitle(title);

            logger.info("Query completed successfully: {} rows, {} columns", table.getRowCount(), table.getColumnCount());

            IExporter exporter = format.getExporter();

            logger.info("Starting export: format={}, output={}", format.name().toLowerCase(Locale.ROOT), output.toAbsolutePath());

            exporter.export(table, output);

            logger.info("Export completed successfully: {}", output.toAbsolutePath());

            return 0;
        } catch (SQLException e) {

            logger.error("Database error: {}", e.getMessage(), e);
            return 1;

        } catch (Exception e) {

            logger.error("Export failed: {}", e.getMessage(), e);
            return 1;
        }
    }
}