package dataexportapp.db_config.database;


public class ProcedureParameter {

    private final String name;
    private final int position;
    private final String inOut; // "IN", "OUT", or "IN/OUT"
    private final String dataType;

    public ProcedureParameter(String name, int position, String inOut, String dataType) {
        this.name = name;
        this.position = position;
        this.inOut = inOut;
        this.dataType = dataType;
    }

    public String getName() { return name; }
    public int getPosition() { return position; }
    public String getInOut() { return inOut; }
    public String getDataType() { return dataType; }

    public boolean isOutCursor() {
        return inOut != null && inOut.contains("OUT") && "REF CURSOR".equalsIgnoreCase(dataType);
    }
    public boolean isInput() {
        return inOut != null && inOut.contains("IN");
    }
}