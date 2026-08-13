package dataexportapp.db_config.validation;

public class SQLValidator {

    public static void validate(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL query cannot be empty."
            );
        }

        String normalized = sql.trim().toUpperCase();

        // Remove a trailing semicolon
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            ).trim();
        }

        // Allowed: SELECT
        if (normalized.startsWith("SELECT ")) {
            return;
        }

        // Allowed: CALL
        if (normalized.startsWith("CALL ")) {
            return;
        }

        // Allowed: JDBC CALL syntax
        if (normalized.startsWith("{CALL ")) {
            return;
        }

        // Allowed: Oracle PL/SQL block
        if (normalized.startsWith("BEGIN ")) {
            return;
        }

        throw new IllegalArgumentException(
                "Only SELECT statements and stored procedure calls are allowed."
        );
    }
}