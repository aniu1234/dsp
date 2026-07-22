package com.qinyadan.system.dsp.storage.api.service;

import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.EngineRegistry;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.meta.MetadataStore;
import com.qinyadan.system.dsp.storage.api.meta.OpenTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of storage engines per table.
 * Opens engines lazily on first access and closes them on demand.
 */
public class StorageManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StorageManager.class);

    private final MetadataStore metadata;
    private final Path storageRoot;
    private final Map<String, List<StorageEngine>> openEngines = new ConcurrentHashMap<>();

    public StorageManager(MetadataStore metadata) {
        this(metadata, defaultStorageRoot());
    }

    public StorageManager(MetadataStore metadata, Path storageRoot) {
        if (metadata == null || storageRoot == null) {
            throw new IllegalArgumentException("Metadata store and storage root are required");
        }
        this.metadata = metadata;
        this.storageRoot = storageRoot;
    }

    /**
     * Open (or reuse) a storage engine for the given table request.
     */
    public List<StorageEngine> openTable(OpenTableRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Open table request is required");
        }
        String key = request.getSchema() + "." + request.getTableName();
        return openEngines.computeIfAbsent(key, k -> {
            LOG.info("Opening storage engine for table: {}", key);
            TableInfo table = metadata.getTable(request.getSchema(), request.getTableName());
            if (table == null) {
                throw new IllegalArgumentException("Unknown table: " + key);
            }
            if (table.getShards() < 1) {
                throw new IllegalArgumentException("Table must have at least one shard: " + key);
            }
            List<StorageEngine> engines = new ArrayList<>();
            try {
                for (int shard = 0; shard < table.getShards(); shard++) {
                    Path path = storageRoot.resolve(table.getSchema()).resolve(table.getName())
                            .resolve(Integer.toString(shard));
                    EngineConfig config = new EngineConfig(path.toString(), table.getColumns(),
                            Collections.<String, Object>singletonMap("shard", shard));
                    engines.add(EngineRegistry.create(table.getEngine(), config));
                }
                return Collections.unmodifiableList(engines);
            } catch (RuntimeException e) {
                engines.forEach(StorageEngine::close);
                throw e;
            }
        });
    }

    /**
     * Close and release a storage engine for the given table.
     */
    public void closeTable(String schema, String table) {
        String key = schema + "." + table;
        List<StorageEngine> engines = openEngines.remove(key);
        if (engines != null) {
            LOG.info("Closing storage engine for table: {}", key);
            engines.forEach(StorageEngine::close);
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
        return new ArrayList<>(openEngines.keySet());
    }

    @Override
    public void close() {
        openEngines.values().forEach(engines -> engines.forEach(StorageEngine::close));
        openEngines.clear();
    }

    private static Path defaultStorageRoot() {
        String configured = System.getProperty("dsp.data.dir");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("DSP_DATA_DIR");
        }
        if (configured == null || configured.trim().isEmpty()) {
            configured = Paths.get(System.getProperty("user.home"), "test", "sloth").toString();
        }
        return Paths.get(configured);
    }
}
