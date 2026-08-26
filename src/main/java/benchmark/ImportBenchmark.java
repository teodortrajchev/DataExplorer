package benchmark;

import dataexploreapp.dataexport.exporters.CSVExporter;
import dataexploreapp.dataexport.exporters.ExcelExporter;
import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.importfiles.DataImportService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ImportBenchmark {

    @Param({"1000", "10000", "50000"})
    public int rowCount;

    private Path csvFile;
    private Path xlsxFile;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        DataTable table = BenchmarkDataFactory.buildEmployeeTable(rowCount);
        Path dir = Files.createTempDirectory("jmh-import");
        csvFile = dir.resolve("employees.csv");
        xlsxFile = dir.resolve("employees.xlsx");
        new CSVExporter().export(table, csvFile);
        new ExcelExporter().export(table, xlsxFile);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        Files.deleteIfExists(csvFile);
        Files.deleteIfExists(xlsxFile);
    }

    @Benchmark
    public void csvImport(Blackhole bh) throws IOException {
        bh.consume(DataImportService.importFile(csvFile.toFile()));
    }

    @Benchmark
    public void excelImport(Blackhole bh) throws IOException {
        bh.consume(DataImportService.importFile(xlsxFile.toFile()));
    }
}