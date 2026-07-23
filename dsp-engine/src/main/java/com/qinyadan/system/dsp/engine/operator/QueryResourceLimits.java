package com.qinyadan.system.dsp.engine.operator;

/**
 * Central guard for operators that must materialize rows in memory.
 */
public final class QueryResourceLimits {

    private static final int DEFAULT_MAX_MATERIALIZED_ROWS = 100000;

    private QueryResourceLimits() {
    }

    public static void checkMaterializedRows(long rowCount, String operator) {
        int maximum = maxMaterializedRows();
        if (rowCount > maximum) {
            throw new QueryResourceLimitException(operator + " exceeded the in-memory row limit of " + maximum);
        }
    }

    public static int maxMaterializedRows() {
        String configured = System.getProperty("dsp.query.max-materialized-rows");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("DSP_QUERY_MAX_MATERIALIZED_ROWS");
        }
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_MAX_MATERIALIZED_ROWS;
        }
        try {
            int value = Integer.parseInt(configured);
            if (value < 1) {
                throw new IllegalArgumentException("Materialized row limit must be positive");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid materialized row limit: " + configured, e);
        }
    }
}
