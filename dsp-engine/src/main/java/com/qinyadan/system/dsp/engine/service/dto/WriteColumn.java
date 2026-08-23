package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.core.data.type.DataType;

/**
 * Immutable column contract exposed by the write application service.
 */
public final class WriteColumn {

    private final String name;
    private final DataType<?> dataType;
    private final boolean nullable;
    private final boolean unsigned;
    private final int precision;
    private final String defaultValue;

    public WriteColumn(String name, DataType<?> dataType, boolean nullable,
                       boolean unsigned, int precision, String defaultValue) {
        if (name == null || name.trim().isEmpty() || dataType == null) {
            throw new IllegalArgumentException("Write column name and data type are required");
        }
        this.name = name;
        this.dataType = dataType;
        this.nullable = nullable;
        this.unsigned = unsigned;
        this.precision = precision;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public DataType<?> getDataType() {
        return dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public int getPrecision() {
        return precision;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
