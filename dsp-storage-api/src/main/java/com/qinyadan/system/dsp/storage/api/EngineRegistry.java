package com.qinyadan.system.dsp.storage.api;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EngineRegistry {

    private static final Map<String, StorageEngineFactory> FACTORIES = new ConcurrentHashMap<>();

    static {
        ServiceLoader.load(StorageEngineFactory.class).forEach(EngineRegistry::register);
    }

    private EngineRegistry() {
    }

    public static void register(StorageEngineFactory factory) {
        if (factory == null || factory.type() == null || factory.type().trim().isEmpty()) {
            throw new IllegalArgumentException("Storage engine factory and type are required");
        }
        FACTORIES.put(factory.type().toLowerCase(Locale.ROOT), factory);
    }

    public static StorageEngine create(String type, EngineConfig config) {
        if (type == null) {
            throw new IllegalArgumentException("Storage engine type is required");
        }
        StorageEngineFactory factory = FACTORIES.get(type.toLowerCase(Locale.ROOT));
        if (factory == null) {
            throw new IllegalStateException("No engine factory registered for type: " + type);
        }
        return factory.create(config);
    }

    public static Set<String> availableTypes() {
        return Collections.unmodifiableSet(FACTORIES.keySet());
    }
}
