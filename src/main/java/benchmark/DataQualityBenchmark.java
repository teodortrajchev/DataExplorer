package benchmark;

import dataexploreapp.db_config.dataquality.DataQualityAnalyzer;
import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Mode.AverageTime;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class DataQualityBenchmark {

    @Param({"1000", "10000", "100000"})
    public int rowCount;

    private List<Map<String, Object>> rows;
    private DataQualityAnalyzer analyzer;

    @Setup
    public void setup() {
        analyzer = new DataQualityAnalyzer();
        rows = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            row.put("email", "user" + i + "@example.com");
            rows.add(row);
        }
    }

    @Benchmark
    public Object analyze10kRows() {
        return analyzer.analyze(rows);
    }
}