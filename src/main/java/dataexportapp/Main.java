package dataexportapp;

import dataexportapp.db_config.database.DataBaseReader;
import dataexportapp.db_config.database.DataTable;
import dataexportapp.dataexport.exporters.ExportFormat;
import dataexportapp.dataexport.exporters.IExporter;
import io.github.cdimascio.dotenv.Dotenv;
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

            Dotenv dotenv = Dotenv.load();

            String DB_URL = dotenv.get("DB_URL");
            String DB_USER = dotenv.get("DB_USER");
            String DB_PASS = dotenv.get("DB_PASS");
            DataBaseReader reader = new DataBaseReader(DB_URL,DB_USER,DB_PASS);

            System.out.println("Running query...");
            DataTable table = reader.runQuery(query);
            table.setTitle(title);
            System.out.printf("Fetched %d rows, %d columns.%n", table.getRowCount(), table.getColumnCount());

            IExporter exporter = format.getExporter();
            System.out.println("Exporting to " + format.name().toLowerCase(Locale.ROOT) + "...");
            exporter.export(table, output);

            System.out.println("Done: " + output.toAbsolutePath());
            return 0;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}