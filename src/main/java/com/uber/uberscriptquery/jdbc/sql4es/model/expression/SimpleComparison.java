package com.uber.uberscriptquery.jdbc.sql4es.model.expression;

import com.facebook.presto.sql.tree.ComparisonExpressionType;
import com.uber.uberscriptquery.jdbc.sql4es.model.Column;

import java.sql.SQLException;
import java.util.List;

public class SimpleComparison implements IComparison {

    private Column leftColumn;
    private ComparisonExpressionType comparisonType;
    private Number rightValue;
    private Column rightColumn;

    public SimpleComparison(Column column, ComparisonExpressionType comparisonType, Number value) {
        super();
        this.leftColumn = column;
        this.comparisonType = comparisonType;
        this.rightValue = value;
    }

    public SimpleComparison(Column leftColumn, ComparisonExpressionType comparisonType, Column rightColumn) {
        super();
        this.leftColumn = leftColumn;
        this.comparisonType = comparisonType;
        this.rightColumn = rightColumn;
    }

    public String toString() {
        return leftColumn.getFullName() + " " + comparisonType + " " + rightValue + " (" + rightValue.getClass().getSimpleName() + ")";
    }

    @Override
    public boolean evaluate(List<Object> row) throws SQLException {
        if (leftColumn.getIndex() >= row.size()) throw new SQLException("Unable to filter row, index " + leftColumn.getIndex() + " is out of bounds");
        try {
            Double leftValue = null;
            Double rightValue = null;
            Object leftObject = row.get(leftColumn.getIndex());
            if (!(leftObject instanceof Number))
                throw new SQLException("Unable to filter row because value '" + leftObject + "' has unknown type " + leftObject.getClass().getSimpleName());
            leftValue = ((Number) leftObject).doubleValue();

            if (this.rightValue != null) {
                rightValue = this.rightValue.doubleValue();
            } else {
                Object colValue = row.get(rightColumn.getIndex());
                if (!(colValue instanceof Number))
                    throw new SQLException("Unable to filter row because value '" + colValue + "' has unknown type " + colValue.getClass().getSimpleName());
                this.rightValue = (Number) colValue;
                rightValue = this.rightValue.doubleValue();
            }

            if (this.comparisonType == ComparisonExpressionType.EQUAL) return leftValue.equals(rightValue);
            if (this.comparisonType == ComparisonExpressionType.GREATER_THAN) return leftValue > rightValue;
            if (this.comparisonType == ComparisonExpressionType.GREATER_THAN_OR_EQUAL) return leftValue >= rightValue;
            if (this.comparisonType == ComparisonExpressionType.LESS_THAN) return leftValue < rightValue;
            if (this.comparisonType == ComparisonExpressionType.LESS_THAN_OR_EQUAL) return leftValue <= rightValue;
        } catch (Exception e) {
            throw new SQLException("Unable to filter row because: " + e.getMessage(), e);
        }
        return false;
    }

}
