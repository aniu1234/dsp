package com.qinyadan.system.dsp.schema.hive;

import com.qinyadan.system.dsp.schema.common.spi.ConnectorHealth;
import com.qinyadan.system.dsp.schema.common.spi.SchemaConnector;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

import java.util.Map;


public class HiveSchemaFactory implements SchemaConnector {

    @Override
    public String id() {
        return "hive";
    }

    @Override
    public ConnectorHealth health(Map<String, Object> operand) {
        return ConnectorHealth.unavailable("Hive schema connector is not implemented");
    }

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        throw new UnsupportedOperationException(
                "Hive schema connector is not implemented; do not configure it in a model");
    }
}
