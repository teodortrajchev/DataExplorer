package dataexploreapp.filtering;

/** One filter row: a column, an operator, and the value(s) to compare against. */
public class FilterCondition {
    private final String column;
    private final FilterOperator operator;
    private final String value;
    private final String secondValue; // only used by BETWEEN

    public FilterCondition(String column, FilterOperator operator, String value, String secondValue) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.secondValue = secondValue;
    }

    public FilterCondition(String column, FilterOperator operator, String value) {
        this(column, operator, value, null);
    }

    public String getColumn() { return column; }
    public FilterOperator getOperator() { return operator; }
    public String getValue() { return value; }
    public String getSecondValue() { return secondValue; }

    public String describe() {
        return operator.requiresSecondValue()
                ? column + " " + operator + " " + value + " and " + secondValue
                : column + " " + operator + " " + value;
    }
}