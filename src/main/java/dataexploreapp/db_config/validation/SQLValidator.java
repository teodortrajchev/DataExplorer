package dataexploreapp.db_config.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLValidator {
    private static final Logger logger =
            LoggerFactory.getLogger(SQLValidator.class);
    public static void validate(String sql) {

        logger.debug("Validating SQL statement");

        if (sql == null || sql.isBlank()) {

            logger.warn("SQL validation failed: query is empty");

            throw new IllegalArgumentException(
                    "SQL query cannot be empty."
            );
        }

        String normalized = sql.trim().toUpperCase();

        if (normalized.endsWith(";")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            ).trim();
        }

        if (normalized.startsWith("SELECT ")) {

            logger.debug("SQL validation successful: SELECT");

            return;
        }

        if (normalized.startsWith("CALL ")) {

            logger.debug("SQL validation successful: CALL");

            return;
        }

        if (normalized.startsWith("{CALL ")) {

            logger.debug("SQL validation successful: JDBC CALL");

            return;
        }

        if (normalized.startsWith("BEGIN ")) {

            logger.debug("SQL validation successful: PL/SQL block");

            return;
        }

        logger.warn("SQL validation failed: unsupported SQL statement");

        throw new IllegalArgumentException(
                "Only SELECT statements and stored procedure calls are allowed."
        );
    }
}