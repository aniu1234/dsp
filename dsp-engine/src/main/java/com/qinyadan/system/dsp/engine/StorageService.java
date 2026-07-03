package com.qinyadan.system.dsp.engine;

import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.schema.Table;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qinyadan.system.dsp.engine.util.ThreadPoolUtil.FLUSH_POOL;
import static com.qinyadan.system.dsp.engine.util.ThreadPoolUtil.SCHEDULE_POOL;


/**
 * Storage lifecycle coordinator.
 * Periodically checks all open storage engines and flushes those that need it.
 */
@Slf4j
public class StorageService implements LifeCycle {

    public static final StorageService INSTANCE = new StorageService();

    @Override
    public void init() {
        scheduleFlushData();
    }

    /**
     * Schedule a periodic task that checks and flushes storage engines.
     */
    public void scheduleFlushData() {
        log.info("start period thread to check storage engine");
        SCHEDULE_POOL.scheduleAtFixedRate(() -> {
            final List<com.qinyadan.system.dsp.storage.StorageEngine> storageEngines = getShouldFlush();
            storageEngines.forEach(storageEngine -> FLUSH_POOL.submit(storageEngine::flush));
        }, 5, 5, TimeUnit.SECONDS);
    }


    /**
     * Collect all storage engines across all schemas and tables that need flushing.
     */
    private List<com.qinyadan.system.dsp.storage.StorageEngine> getShouldFlush() {
        log.info("Start to check if any table needs to flush data...");
        return SlothSchemaHolder.INSTANCE.getSchemaMap().stream()
                .flatMap(schema -> schema.getAllTable().stream())
                .map(table -> ((SlothTable) table).getSlothTableEngine())
                .filter(engine -> engine != null)
                .flatMap(tableEngine -> tableEngine.getStorageEngines().stream())
                .filter(com.qinyadan.system.dsp.storage.StorageEngine::shouldFlush)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        //do nothing
    }
}
