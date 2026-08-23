package com.qinyadan.system.dsp.engine.service.dto;

/**
 * Framework-neutral UPDATE assignment.
 */
public final class MutationAssignment {

    private final String column;
    private final String expressionSql;
    private final boolean defaultValue;
    private final boolean nullValue;

    public MutationAssignment(String column, String expressionSql,
                              boolean defaultValue, boolean nullValue) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Mutation column is required");
        }
        if (!defaultValue && !nullValue
                && (expressionSql == null || expressionSql.trim().isEmpty())) {
            throw new IllegalArgumentException("Mutation expression is required");
        }
        this.column = column;
        this.expressionSql = expressionSql;
        this.defaultValue = defaultValue;
        this.nullValue = nullValue;
    }

    public String getColumn() { return column; }
    public String getExpressionSql() { return expressionSql; }
    public boolean isDefaultValue() { return defaultValue; }
    public boolean isNullValue() { return nullValue; }
}
