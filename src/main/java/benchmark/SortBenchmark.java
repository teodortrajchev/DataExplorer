package benchmark;

import dataexploreapp.db_config.database.DataTable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class SortBenchmark {

    @Param({"1000", "10000", "100000"})
    public int rowCount;

    private DataTable table;

    @Setup(Level.Trial)
    public void setUp() {
        table = BenchmarkDataFactory.buildEmployeeTable(rowCount);
    }

    @Benchmark
    public void sortByNumericColumnAscending(Blackhole bh) {
        bh.consume(table.sortBy("salary", true));
    }

    @Benchmark
    public void sortByTextColumnAscending(Blackhole bh) {
        bh.consume(table.sortBy("name", true));
    }

    @Benchmark
    public void sortByTextColumnDescending(Blackhole bh) {
        bh.consume(table.sortBy("department", false));
    }
}