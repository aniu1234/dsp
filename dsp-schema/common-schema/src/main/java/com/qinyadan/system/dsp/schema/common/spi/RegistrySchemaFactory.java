package com.qinyadan.system.dsp.schema.common.spi;

import com.qinyadan.system.dsp.schema.common.config.SchemaConfig;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Stable Calcite model entry point that resolves concrete factories by
 * {@code operand.connector} instead of a connector implementation class name.
 */
public final class RegistrySchemaFactory implements SchemaFactory {

    public static final String CONNECTOR_OPTION = "connector";

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        String connectorId = SchemaConfig.requireString(operand, CONNECTOR_OPTION);
        return SchemaConnectorRegistry.load().create(
                connectorId, parentSchema, name, connectorOptions(operand));
    }

    private Map<String, Object> connectorOptions(Map<String, Object> operand) {
        Map<String, Object> options = new LinkedHashMap<>(operand);
        options.remove(CONNECTOR_OPTION);
        return Collections.unmodifiableMap(options);
    }
}
