package com.qinyadan.system.dsp.schema.hive;

import com.qinyadan.system.dsp.schema.common.spi.ConnectorHealth;
import com.qinyadan.system.dsp.schema.common.spi.SchemaConnectorRegistry;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class HiveSchemaFactoryTest {

    @Test(expected = UnsupportedOperationException.class)
    public void failsFastUntilHiveConnectorIsImplemented() {
        new HiveSchemaFactory().create(null, "hive",
                Collections.<String, Object>emptyMap());
    }

    @Test
    public void exposesUnavailableHealthInsteadOfHidingConnector() {
        ConnectorHealth health = new HiveSchemaFactory().health(
                Collections.<String, Object>emptyMap());

        assertFalse(health.isReady());
    }

    @Test
    public void discoversHiveConnectorThroughServiceLoader() {
        SchemaConnectorRegistry registry = SchemaConnectorRegistry.load(
                getClass().getClassLoader());

        assertTrue(registry.ids().contains("hive"));
    }
}
