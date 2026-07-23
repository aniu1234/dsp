package com.qinyadan.system.dsp.schema.common.config;

import java.util.Map;


public final class SchemaConfig {

    private SchemaConfig() {
    }

    public static String requireString(Map<String, Object> operand, String key) {
        String value = optionalString(operand, key, null);
        if (value == null) {
            throw new IllegalArgumentException("Schema option '" + key + "' is required");
        }
        return value;
    }

    public static String optionalString(Map<String, Object> operand, String key,
                                        String defaultValue) {
        if (operand == null) {
            return defaultValue;
        }
        Object rawValue = operand.get(key);
        if (rawValue == null) {
            return defaultValue;
        }
        String value = rawValue.toString().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    public static String resolve(String property, String environment, String fallback) {
        String value = trimToNull(System.getProperty(property));
        if (value == null) {
            value = trimToNull(System.getenv(environment));
        }
        return value == null ? trimToNull(fallback) : value;
    }

    public static String requireResolved(String option, String property,
                                         String environment, String fallback) {
        String value = resolve(property, environment, fallback);
        if (value == null) {
            throw new IllegalArgumentException("Schema option '" + option + "' is required");
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
