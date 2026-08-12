package Config.Database;


public class ForeignKeyInfo {
    private final String constraintName;
    private final String fkColumn;
    private final String referencedOwner;
    private final String referencedTable;
    private final String referencedColumn;

    public ForeignKeyInfo(String constraintName, String fkColumn,
                          String referencedOwner, String referencedTable, String referencedColumn) {
        this.constraintName = constraintName;
        this.fkColumn = fkColumn;
        this.referencedOwner = referencedOwner;
        this.referencedTable = referencedTable;
        this.referencedColumn = referencedColumn;
    }

    public String getConstraintName() { return constraintName; }
    public String getFkColumn() { return fkColumn; }
    public String getReferencedOwner() { return referencedOwner; }
    public String getReferencedTable() { return referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }

    public String describe() {
        return fkColumn + " → " + referencedTable + "." + referencedColumn;
    }
}