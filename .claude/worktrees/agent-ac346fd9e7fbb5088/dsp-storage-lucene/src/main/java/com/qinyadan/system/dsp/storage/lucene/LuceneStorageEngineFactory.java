package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.StorageEngineFactory;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<ColumnInfo> columnInfos = config.getColumnTypes().stream()
                .map(t -> new ColumnInfo("col_" + t.id(), t, true, null, ""))
                .collect(Collectors.toList());

        Map<String, DataType<?>> typeMap = columnInfos.stream()
                .collect(Collectors.toMap(ColumnInfo::getName, ColumnInfo::getType));

        LuceneStorageEngine engine = new LuceneStorageEngine(
                config.getStoragePath(),
                columnInfos,
                typeMap
        );
        engine.init();
        return engine;
    }
}
