package dataexploreapp.db_config.dataquality;

public class DataQualityReport {

    private int duplicateRows;
    private double missingPercentage;
    private int invalidEmails;
    private double score;

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(int duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public double getMissingPercentage() {
        return missingPercentage;
    }

    public void setMissingPercentage(double missingPercentage) {
        this.missingPercentage = missingPercentage;
    }

    public int getInvalidEmails() {
        return invalidEmails;
    }

    public void setInvalidEmails(int invalidEmails) {
        this.invalidEmails = invalidEmails;
    }


    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}