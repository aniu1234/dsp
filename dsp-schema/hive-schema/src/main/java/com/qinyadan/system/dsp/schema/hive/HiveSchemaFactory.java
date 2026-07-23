package com.qinyadan.system.dsp.schema.hive;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

import java.util.Map;


public class HiveSchemaFactory implements SchemaFactory {
    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        throw new UnsupportedOperationException(
                "Hive schema connector is not implemented; do not configure it in a model");
    }
}
