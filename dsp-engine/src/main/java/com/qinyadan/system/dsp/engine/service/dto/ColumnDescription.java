package com.qinyadan.system.dsp.engine.service.dto;

/**
 * Framework-neutral catalog column response.
 */
public final class ColumnDescription {

    private final String name;
    private final String sqlType;
    private final boolean unsigned;
    private final boolean nullable;
    private final String defaultValue;
    private final String comment;
    private final int precision;

    public ColumnDescription(String name, String sqlType, boolean unsigned,
                             boolean nullable, String defaultValue,
                             String comment, int precision) {
        this.name = name;
        this.sqlType = sqlType;
        this.unsigned = unsigned;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.precision = precision;
    }

    public String getName() { return name; }
    public String getSqlType() { return sqlType; }
    public boolean isUnsigned() { return unsigned; }
    public boolean isNullable() { return nullable; }
    public String getDefaultValue() { return defaultValue; }
    public String getComment() { return comment; }
    public int getPrecision() { return precision; }
}
