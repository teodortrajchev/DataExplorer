package benchmark;

import dataexploreapp.db_config.database.DataTable;
import dataexploreapp.filtering.DataFilterEngine;
import dataexploreapp.filtering.FilterCombinator;
import dataexploreapp.filtering.FilterCondition;
import dataexploreapp.filtering.FilterOperator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FilterBenchmark {

    @Param({"1000", "10000", "100000"})
    public int rowCount;

    private DataTable table;
    private List<FilterCondition> singleContains;
    private List<FilterCondition> multiAnd;
    private List<FilterCondition> numericBetween;

    @Setup(Level.Trial)
    public void setUp() {
        table = BenchmarkDataFactory.buildEmployeeTable(rowCount);

        singleContains = List.of(new FilterCondition("department", FilterOperator.CONTAINS, "eng"));

        multiAnd = List.of(
                new FilterCondition("department", FilterOperator.CONTAINS, "e"),
                new FilterCondition("salary", FilterOperator.GREATER_OR_EQUAL, "60000"),
                new FilterCondition("active", FilterOperator.EQUALS, "true"));

        numericBetween = List.of(new FilterCondition("salary", FilterOperator.BETWEEN, "50000", "90000"));
    }

    @Benchmark
    public void singleContainsFilter(Blackhole bh) {
        bh.consume(DataFilterEngine.apply(table, singleContains, FilterCombinator.AND));
    }

    @Benchmark
    public void multiConditionAndFilter(Blackhole bh) {
        bh.consume(DataFilterEngine.apply(table, multiAnd, FilterCombinator.AND));
    }

    @Benchmark
    public void numericBetweenFilter(Blackhole bh) {
        bh.consume(DataFilterEngine.apply(table, numericBetween, FilterCombinator.AND));
    }

    @Benchmark
    public void dataTableFilterContains(Blackhole bh) {
        bh.consume(table.filterContains("department", "eng"));
    }
}