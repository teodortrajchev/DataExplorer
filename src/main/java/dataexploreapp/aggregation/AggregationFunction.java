package dataexploreapp.aggregation;


public enum AggregationFunction {
    SUM, AVG, COUNT, MIN, MAX;

    // vrakja "SUM(SALARY)"
    public String describe(String valueColumn) {
        if (this == COUNT) {
            return "COUNT";
        }
        return this + "(" + valueColumn + ")";
    }

    //Count nema potreba od value column samo gi broi redicite
    public boolean requiresValueColumn() {
        return this != COUNT;
    }
}