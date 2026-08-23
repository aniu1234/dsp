package com.qinyadan.system.dsp.schema.mysql;

import com.qinyadan.system.dsp.schema.common.spi.SchemaConnectorRegistry;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class MysqlSchemaTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankSchemaNameBeforeOpeningConnection() {
        new MysqlSchema("jdbc:mysql://localhost/test", "user", "password", "  ");
    }

    @Test
    public void discoversMysqlConnectorThroughServiceLoader() {
        SchemaConnectorRegistry registry = SchemaConnectorRegistry.load(
                getClass().getClassLoader());

        assertTrue(registry.ids().contains("mysql"));
    }
}
