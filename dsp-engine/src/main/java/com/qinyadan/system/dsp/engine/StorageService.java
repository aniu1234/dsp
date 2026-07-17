package com.qinyadan.system.dsp.engine;

import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.schema.Table;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
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
    private ScheduledFuture<?> flushTask;

    @Override
    public void init() {
        scheduleFlushData();
    }

    /**
     * Schedule a periodic task that checks and flushes storage engines.
     */
    public void scheduleFlushData() {
        log.info("start period thread to check storage engine");
        flushTask = SCHEDULE_POOL.scheduleAtFixedRate(() -> {
            final List<StorageEngine> storageEngines = getShouldFlush();
            storageEngines.forEach(storageEngine -> FLUSH_POOL.submit(storageEngine::flush));
        }, 5, 5, TimeUnit.SECONDS);
    }


    /**
     * Collect all storage engines across all schemas and tables that need flushing.
     */
    private List<StorageEngine> getShouldFlush() {
        log.info("Start to check if any table needs to flush data...");
        return getAllStorageEngines().stream()
                .filter(StorageEngine::shouldFlush)
                .collect(Collectors.toList());
    }

    private List<StorageEngine> getAllStorageEngines() {
        return SlothSchemaHolder.INSTANCE.getSchemaMap().stream()
                .flatMap(schema -> schema.getAllTable().stream())
                .map(table -> ((SlothTable) table).getSlothTableEngine())
                .filter(engine -> engine != null)
                .flatMap(tableEngine -> tableEngine.getStorageEngines().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        if (flushTask != null) {
            flushTask.cancel(false);
        }
        getAllStorageEngines().forEach(StorageEngine::flush);
        SCHEDULE_POOL.shutdown();
        FLUSH_POOL.shutdown();
        try {
            SCHEDULE_POOL.awaitTermination(5, TimeUnit.SECONDS);
            FLUSH_POOL.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down storage executors", e);
        }
    }
}
