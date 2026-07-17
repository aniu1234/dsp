package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.StorageEngineFactory;

/**
 * Factory that creates {@link LuceneStorageEngine} instances from {@link EngineConfig}.
 * Registers itself with type name "lucene".
 */
public class LuceneStorageEngineFactory implements StorageEngineFactory {

    public static final LuceneStorageEngineFactory INSTANCE = new LuceneStorageEngineFactory();

    @Override
    public String type() {
        return "lucene";
    }

    @Override
    public StorageEngine create(EngineConfig config) {
        LuceneStorageEngine engine = new LuceneStorageEngine(
                config.getStoragePath(),
                config.getColumns()
        );
        engine.init();
        return engine;
    }
}
