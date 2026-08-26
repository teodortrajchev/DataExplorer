package benchmark;

import dataexploreapp.dataexport.IExporter;
import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.dataexport.exporters.ExcelExporter;
import dataexploreapp.dataexport.exporters.JSONExporter;
import dataexploreapp.dataexport.exporters.XMLExporter;
import dataexploreapp.db_config.database.DataTable;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ExportBenchmark {

    @Param({"1000", "10000", "100000"})
    public int rowCount;

    private DataTable table;
    private Path outputDir;

    private final IExporter csvExporter = new CSVExporter();
    private final IExporter excelExporter = new ExcelExporter();
    private final IExporter jsonExporter = new JSONExporter();
    private final IExporter xmlExporter = new XMLExporter();

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        table = BenchmarkDataFactory.buildEmployeeTable(rowCount);
        outputDir = Files.createTempDirectory("jmh-export");
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        try (var files = Files.walk(outputDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    @Benchmark
    public void csvExport() {
        exportOrThrow(csvExporter, "bench.csv");
    }

    @Benchmark
    public void excelExport() {
        exportOrThrow(excelExporter, "bench.xlsx");
    }

    @Benchmark
    public void jsonExport() {
        exportOrThrow(jsonExporter, "bench.json");
    }

    @Benchmark
    public void xmlExport() {
        exportOrThrow(xmlExporter, "bench.xml");
    }

    private void exportOrThrow(IExporter exporter, String fileName) {
        try {
            exporter.export(table, outputDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}