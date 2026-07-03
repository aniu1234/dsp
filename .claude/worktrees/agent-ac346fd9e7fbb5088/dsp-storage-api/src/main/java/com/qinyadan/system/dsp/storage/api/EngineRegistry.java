package com.qinyadan.system.dsp.storage.api;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EngineRegistry {

    private static final Map<String, StorageEngineFactory> FACTORIES = new ConcurrentHashMap<>();

    public static void register(StorageEngineFactory factory) {
        FACTORIES.put(factory.type(), factory);
    }

    public static StorageEngine create(String type, EngineConfig config) {
        StorageEngineFactory factory = FACTORIES.get(type);
        if (factory == null) {
            throw new IllegalStateException("No engine factory registered for type: " + type);
        }
        return factory.create(config);
    }

    public static Set<String> availableTypes() {
        return Collections.unmodifiableSet(FACTORIES.keySet());
    }
}
