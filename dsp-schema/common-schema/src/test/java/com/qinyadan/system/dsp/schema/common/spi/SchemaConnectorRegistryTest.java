package com.qinyadan.system.dsp.schema.common.spi;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;


public class SchemaConnectorRegistryTest {

    @Test
    public void normalizesIdsAndDelegatesSchemaCreation() {
        Schema schema = new AbstractSchema();
        TestConnector connector = new TestConnector("mysql", schema, false);
        SchemaConnectorRegistry registry = SchemaConnectorRegistry.of(
                Collections.singletonList(connector));

        assertEquals("[mysql]", registry.ids().toString());
        assertSame(schema, registry.create(" MYSQL ", null, "catalog",
                Collections.<String, Object>emptyMap()));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsCaseInsensitiveDuplicateIds() {
        SchemaConnectorRegistry.of(Arrays.asList(
                new TestConnector("mysql", new AbstractSchema(), false),
                new TestConnector("MYSQL", new AbstractSchema(), false)));
    }

    @Test
    public void isolatesHealthCheckFailures() {
        SchemaConnectorRegistry registry = SchemaConnectorRegistry.of(
                Collections.singletonList(
                        new TestConnector("broken", new AbstractSchema(), true)));

        ConnectorHealth health = registry.health("broken",
                Collections.<String, Object>emptyMap());

        assertFalse(health.isReady());
        assertEquals(ConnectorHealth.Status.UNAVAILABLE, health.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void stableFactoryRequiresConnectorId() {
        new RegistrySchemaFactory().create(null, "catalog",
                Collections.<String, Object>emptyMap());
    }

    private static final class TestConnector implements SchemaConnector {

        private final String id;
        private final Schema schema;
        private final boolean failHealth;

        private TestConnector(String id, Schema schema, boolean failHealth) {
            this.id = id;
            this.schema = schema;
            this.failHealth = failHealth;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ConnectorHealth health(Map<String, Object> operand) {
            if (failHealth) {
                throw new IllegalStateException("expected failure");
            }
            return ConnectorHealth.ready("ready");
        }

        @Override
        public Schema create(SchemaPlus parentSchema, String name,
                             Map<String, Object> operand) {
            return schema;
        }
    }
}
