package benchmark;


import dataexploreapp.aggregation.AggregationFunction;
import dataexploreapp.aggregation.Aggregator;
import dataexploreapp.db_config.database.DataTable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AggregationBenchmark {

    @Param({"1000", "10000", "100000"})
    public int rowCount;

    private DataTable table;

    @Setup(Level.Trial)
    public void setUp() {
        table = BenchmarkDataFactory.buildEmployeeTable(rowCount);
    }

    @Benchmark
    public void sumBySalary(Blackhole bh) {
        bh.consume(Aggregator.aggregate(table, "department", "salary", AggregationFunction.SUM));
    }

    @Benchmark
    public void avgBySalary(Blackhole bh) {
        bh.consume(Aggregator.aggregate(table, "department", "salary", AggregationFunction.AVG));
    }

    @Benchmark
    public void countByDepartment(Blackhole bh) {
        bh.consume(Aggregator.aggregate(table, "department", null, AggregationFunction.COUNT));
    }
}