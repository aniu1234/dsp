package com.qinyadan.system.dsp.engine.operator;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
/**
 * Central guard for operators that must materialize rows in memory.
 */
public final class QueryResourceLimits {

    private QueryResourceLimits() {
    }

    public static void checkMaterializedRows(long rowCount, String operator) {
        int maximum = maxMaterializedRows();
        if (rowCount > maximum) {
            throw new QueryResourceLimitException(operator + " exceeded the in-memory row limit of " + maximum);
        }
    }

    public static int maxMaterializedRows() {
        return DspConfiguration.load().getQueryMaxMaterializedRows();
    }
}
