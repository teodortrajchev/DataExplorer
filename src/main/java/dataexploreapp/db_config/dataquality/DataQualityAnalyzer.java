package dataexploreapp.db_config.dataquality;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class DataQualityAnalyzer {

    public static DataQualityReport analyze(List<Map<String, Object>> rows) {

        DataQualityReport report = new DataQualityReport();

        if (rows == null || rows.isEmpty()) {
            report.setScore(100);
            return report;
        }

        report.setDuplicateRows(findDuplicates(rows));
        report.setMissingPercentage(calculateMissingPercentage(rows));
        report.setInvalidEmails(findInvalidEmails(rows));
        report.setScore(calculateScore(rows, report));

        return report;
    }

    private static int findDuplicates(List<Map<String, Object>> rows) {
        Set<Map<String, Object>> uniqueRows = new HashSet<>();
        int duplicates = 0;

        for (Map<String, Object> row : rows) {
            if (!uniqueRows.add(row)) {
                duplicates++;
            }
        }

        return duplicates;
    }

    private static double calculateMissingPercentage(List<Map<String, Object>> rows) {
        int missing = 0;
        int total = rows.size() * rows.get(0).size();

        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                if (value == null || value.toString().trim().isEmpty()) {
                    missing++;
                }
            }
        }

        return total == 0 ? 0 : (missing * 100.0) / total;
    }
    private static int findInvalidEmails(List<Map<String, Object>> rows) {
        int invalid = 0;
        Pattern pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        for (Map<String, Object> row : rows) {
            Object email = getColumnIgnoreCase(row, "email");
            if (email != null) {
                String value = email.toString().trim();
                if (!pattern.matcher(value).matches()) {
                    invalid++;
                }
            }
        }

        return invalid;
    }
    private static Object getColumnIgnoreCase(Map<String, Object> row, String columnName) {
        for (String key : row.keySet()) {
            if (key.equalsIgnoreCase(columnName)) {
                return row.get(key);
            }
        }

        return null;
    }

    private static double calculateScore(List<Map<String, Object>> rows, DataQualityReport report) {

        int rowCount = rows.size();
        double duplicatePenalty = ((double) report.getDuplicateRows() / rowCount) * 25;
        double missingPenalty = report.getMissingPercentage() * 0.25;
        double emailPenalty = ((double) report.getInvalidEmails() / rowCount) * 25;
        double score = 100 - duplicatePenalty - missingPenalty - emailPenalty;
        return Math.max(0, Math.min(100, score));
    }

}