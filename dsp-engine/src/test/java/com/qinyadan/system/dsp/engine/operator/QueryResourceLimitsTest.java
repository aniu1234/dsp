package com.qinyadan.system.dsp.engine.operator;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryResourceLimitsTest {

    private final String previousLimit =
            System.getProperty("dsp.query.max-materialized-rows");

    @After
    public void restoreLimit() {
        if (previousLimit == null) {
            System.clearProperty("dsp.query.max-materialized-rows");
        } else {
            System.setProperty("dsp.query.max-materialized-rows", previousLimit);
        }
    }

    @Test(expected = QueryResourceLimitException.class)
    public void rejectsRowsBeyondConfiguredMaterializationLimit() {
        System.setProperty("dsp.query.max-materialized-rows", "2");
        assertEquals(2, QueryResourceLimits.maxMaterializedRows());
        QueryResourceLimits.checkMaterializedRows(3, "test operator");
    }
}
