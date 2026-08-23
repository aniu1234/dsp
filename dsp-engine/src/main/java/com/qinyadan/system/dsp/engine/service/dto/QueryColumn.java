package com.qinyadan.system.dsp.engine.service.dto;

/**
 * Planner-neutral result column metadata.
 */
public final class QueryColumn {

    private final String name;
    private final String sqlType;

    public QueryColumn(String name, String sqlType) {
        if (name == null || sqlType == null) {
            throw new IllegalArgumentException("Query column name and type are required");
        }
        this.name = name;
        this.sqlType = sqlType;
    }

    public String getName() {
        return name;
    }

    public String getSqlType() {
        return sqlType;
    }
}
