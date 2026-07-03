package com.qinyadan.system.dsp.storage.api.service;

import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.meta.CreateTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.MetadataStore;
import com.qinyadan.system.dsp.storage.api.meta.OpenTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of storage engines per table.
 * Opens engines lazily on first access and closes them on demand.
 */
public class StorageManager {

    private static final Logger LOG = LoggerFactory.getLogger(StorageManager.class);

    private final MetadataStore metadata;
    private final Map<String, StorageEngine> openEngines = new ConcurrentHashMap<>();

    public StorageManager(MetadataStore metadata) {
        this.metadata = metadata;
    }

    /**
     * Open (or reuse) a storage engine for the given table request.
     */
    public StorageEngine openTable(OpenTableRequest request) {
        String key = request.getSchema() + "." + request.getTableName();
        return openEngines.computeIfAbsent(key, k -> {
            LOG.info("Opening storage engine for table: {}", key);
            // Subclasses or concrete factories should create the appropriate engine.
            // Default stub: log and return null.
            return null;
        });
    }

    /**
     * Close and release a storage engine for the given table.
     */
    public void closeTable(String schema, String table) {
        String key = schema + "." + table;
        StorageEngine engine = openEngines.remove(key);
        if (engine != null) {
            LOG.info("Closing storage engine for table: {}", key);
            engine.close();
        }
    }

    /**
     * Get the underlying metadata store.
     */
    public MetadataStore getMetadata() {
        return metadata;
    }

    /**
     * List all currently open engine keys.
     */
    public List<String> listOpenTables() {
        List<String> newList = new ArrayList<>(openEngines.keySet());
        return newList;
    }
}
