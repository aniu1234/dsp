package com.qinyadan.system.dsp.schema.hive;

import org.junit.Test;

import java.util.Collections;


public class HiveSchemaFactoryTest {

    @Test(expected = UnsupportedOperationException.class)
    public void failsFastUntilHiveConnectorIsImplemented() {
        new HiveSchemaFactory().create(null, "hive",
                Collections.<String, Object>emptyMap());
    }
}
