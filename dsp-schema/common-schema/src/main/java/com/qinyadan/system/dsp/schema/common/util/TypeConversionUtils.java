package com.qinyadan.system.dsp.schema.common.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


public final class TypeConversionUtils {

    private TypeConversionUtils() {
    }

    public static Object toObject(FieldTypeEnum fieldType, Object value) {
        if (value == null || value instanceof String && ((String) value).trim().isEmpty()) {
            return null;
        }
        if (fieldType == null) {
            return value;
        }
        if (fieldType.getJavaClass().isInstance(value)) {
            return value;
        }

        String text = value.toString().trim();
        try {
            switch (fieldType) {
                case STRING:
                    return value.toString();
                case BOOLEAN:
                    if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                        throw new IllegalArgumentException("Expected true or false");
                    }
                    return Boolean.valueOf(text);
                case BYTE:
                    return number(value).byteValueExact();
                case CHAR:
                    if (text.length() != 1) {
                        throw new IllegalArgumentException("Expected one character");
                    }
                    return text.charAt(0);
                case SHORT:
                    return number(value).shortValueExact();
                case INT:
                    return number(value).intValueExact();
                case LONG:
                    return number(value).longValueExact();
                case FLOAT:
                    return Float.valueOf(text);
                case DOUBLE:
                    return Double.valueOf(text);
                case DATE:
                    return Date.valueOf(text);
                case TIME:
                    return Time.valueOf(text);
                case TIMESTAMP:
                    return Timestamp.valueOf(text);
                default:
                    throw new IllegalArgumentException("Unsupported field type " + fieldType);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid " + fieldType.name().toLowerCase()
                    + " value '" + value + "'", e);
        }
    }

    private static BigDecimal number(Object value) {
        return value instanceof BigDecimal
                ? (BigDecimal) value
                : new BigDecimal(value.toString().trim());
    }
}
