package dataexploreapp.aggregation;


public enum AggregationFunction {
    SUM, AVG, COUNT, MIN, MAX;

    /** Column label used in the resulting aggregated DataTable, e.g. "SUM(SALARY)". */
    public String describe(String valueColumn) {
        if (this == COUNT) {
            return "COUNT";
        }
        return this + "(" + valueColumn + ")";
    }

    /** COUNT doesn't need a value column — it just counts rows per group. */
    public boolean requiresValueColumn() {
        return this != COUNT;
    }
}