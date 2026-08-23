package com.qinyadan.system.dsp.schema.common.spi;

import org.apache.calcite.schema.SchemaFactory;

import java.util.Map;


/**
 * Stable extension contract for a Calcite-backed DSP schema connector.
 *
 * <p>Implementations must provide a public no-argument constructor so they can
 * be discovered through {@link java.util.ServiceLoader}.</p>
 */
public interface SchemaConnector extends SchemaFactory {

    /**
     * Stable, case-insensitive connector identifier used by the registry.
     */
    String id();

    /**
     * Checks configuration and external availability without registering a schema.
     * Implementations should return an unavailable result instead of throwing.
     */
    ConnectorHealth health(Map<String, Object> operand);
}
