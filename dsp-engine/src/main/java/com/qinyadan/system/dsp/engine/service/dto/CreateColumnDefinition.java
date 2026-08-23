package com.qinyadan.system.dsp.engine.service.dto;

/**
 * Framework-neutral column definition accepted by the catalog service.
 */
public final class CreateColumnDefinition {

    private final String name;
    private final String sqlType;
    private final boolean unsigned;
    private final boolean nullable;
    private final String defaultValue;
    private final String comment;
    private final int precision;

    public CreateColumnDefinition(String name, String sqlType, boolean unsigned,
                                  boolean nullable, String defaultValue,
                                  String comment, int precision) {
        if (name == null || name.trim().isEmpty()
                || sqlType == null || sqlType.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name and SQL type are required");
        }
        this.name = name;
        this.sqlType = sqlType;
        this.unsigned = unsigned;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.precision = precision;
    }

    public String getName() {
        return name;
    }

    public String getSqlType() {
        return sqlType;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getComment() {
        return comment;
    }

    public int getPrecision() {
        return precision;
    }
}
