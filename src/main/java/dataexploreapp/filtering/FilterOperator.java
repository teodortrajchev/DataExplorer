package dataexploreapp.filtering;

public enum FilterOperator {
    CONTAINS("contains"),
    EQUALS("="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    BETWEEN("between");

    private final String symbol;

    FilterOperator(String symbol) {
        this.symbol = symbol;
    }

    /** True for BETWEEN, the only operator needing a second value field. */
    public boolean requiresSecondValue() {
        return this == BETWEEN;
    }

    @Override
    public String toString() {
        return symbol;
    }
}