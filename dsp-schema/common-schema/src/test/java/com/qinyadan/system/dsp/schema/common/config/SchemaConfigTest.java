package com.qinyadan.system.dsp.schema.common.config;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class SchemaConfigTest {

    @Test
    public void readsAndTrimsSchemaOptions() {
        Map<String, Object> operand = new HashMap<>();
        operand.put("directory", "  csv  ");

        assertEquals("csv", SchemaConfig.requireString(operand, "directory"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRequiredOption() {
        SchemaConfig.requireString(Collections.<String, Object>emptyMap(), "directory");
    }
}
