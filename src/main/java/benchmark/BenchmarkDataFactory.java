package benchmark;

import dataexploreapp.db_config.database.DataTable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

//generira podatoci za benchmarks

public final class BenchmarkDataFactory {

    private static final String[] DEPARTMENTS = {"Engineering", "Sales", "Marketing", "Finance", "Support", "Operations", "HR", "Legal"};

    private BenchmarkDataFactory() {
    }

    public static DataTable buildEmployeeTable(int rowCount) {
        return buildEmployeeTable(rowCount, 42L);
    }

    public static DataTable buildEmployeeTable(int rowCount, long seed) {
        Random random = new Random(seed);
        DataTable table = new DataTable(List.of("id", "name", "department", "salary", "hireDate", "active"));
        table.setTitle("Employees");

        long dayMillis = 24L * 60 * 60 * 1000;
        long baseTime = System.currentTimeMillis() - (10L * 365 * dayMillis);

        for (int i = 0; i < rowCount; i++) {
            String name = "Employee_" + i;
            String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
            double salary = 35_000 + random.nextInt(120_000);
            Timestamp hireDate = new Timestamp(baseTime + (long) (random.nextDouble() * 10 * 365 * dayMillis));
            boolean active = random.nextInt(10) != 0; // ~90% active

            table.addRow(new Object[]{i, name, department, salary, hireDate, active});
        }
        return table;
    }
}