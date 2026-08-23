package com.qinyadan.system.dsp.schema.common.spi;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;


public final class SchemaConnectorRegistry {

    private static final Pattern CONNECTOR_ID = Pattern.compile("[a-z][a-z0-9-]*");

    private final Map<String, SchemaConnector> connectors;

    private SchemaConnectorRegistry(Iterable<? extends SchemaConnector> providers) {
        Map<String, SchemaConnector> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SchemaConnector provider : providers) {
            if (provider == null) {
                throw new IllegalArgumentException("Schema connector provider must not be null");
            }
            String id = normalizeId(provider.id());
            SchemaConnector duplicate = sorted.put(id, provider);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate schema connector id '" + id
                        + "': " + duplicate.getClass().getName() + " and "
                        + provider.getClass().getName());
            }
        }
        connectors = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    public static SchemaConnectorRegistry load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SchemaConnectorRegistry.class.getClassLoader();
        }
        return load(classLoader);
    }

    public static SchemaConnectorRegistry load(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("ClassLoader must not be null");
        }
        List<SchemaConnector> providers = new ArrayList<>();
        try {
            for (SchemaConnector connector : ServiceLoader.load(SchemaConnector.class, classLoader)) {
                providers.add(connector);
            }
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException("Unable to load schema connectors", error);
        }
        return new SchemaConnectorRegistry(providers);
    }

    public static SchemaConnectorRegistry of(Iterable<? extends SchemaConnector> providers) {
        if (providers == null) {
            throw new IllegalArgumentException("Schema connector providers must not be null");
        }
        return new SchemaConnectorRegistry(providers);
    }

    public Set<String> ids() {
        return connectors.keySet();
    }

    public Map<String, SchemaConnector> connectors() {
        return connectors;
    }

    public SchemaConnector require(String id) {
        String normalized = normalizeId(id);
        SchemaConnector connector = connectors.get(normalized);
        if (connector == null) {
            throw new IllegalArgumentException("Unknown schema connector '" + normalized
                    + "'; available connectors: " + connectors.keySet());
        }
        return connector;
    }

    public ConnectorHealth health(String id, Map<String, Object> operand) {
        SchemaConnector connector = require(id);
        try {
            ConnectorHealth health = connector.health(emptyIfNull(operand));
            return health == null
                    ? ConnectorHealth.unavailable("Connector returned no health result")
                    : health;
        } catch (RuntimeException e) {
            return ConnectorHealth.unavailable(
                    "Connector '" + normalizeId(id) + "' health check failed", e);
        }
    }

    public Schema create(String connectorId, SchemaPlus parentSchema, String schemaName,
                         Map<String, Object> operand) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name must not be empty");
        }
        return require(connectorId).create(parentSchema, schemaName.trim(), emptyIfNull(operand));
    }

    private static Map<String, Object> emptyIfNull(Map<String, Object> operand) {
        return operand == null ? Collections.<String, Object>emptyMap() : operand;
    }

    private static String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Schema connector id must not be null");
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (!CONNECTOR_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid schema connector id '" + id
                    + "'; expected " + CONNECTOR_ID.pattern());
        }
        return normalized;
    }
}
