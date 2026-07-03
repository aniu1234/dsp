package com.qinyadan.system.dsp.storage;

import com.qinyadan.system.dsp.runtime.LifeCycle;
import com.qinyadan.system.dsp.runtime.util.ThreadPoolUtil;
import com.qinyadan.system.dsp.storage.parser.SlothSchemaHolder;
import com.qinyadan.system.dsp.storage.parser.SlothTable;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qinyadan.system.dsp.runtime.util.ThreadPoolUtil.FLUSH_POOL;


@Slf4j
public class StorageService implements LifeCycle {

    public static final StorageService INSTANCE = new StorageService();

    @Override
    public void init() {
        scheduleFlushData();
    }

    public void scheduleFlushData() {
        log.info("start peroid thread to check storage engine");
        ThreadPoolUtil.SCHEDULE_POOL.scheduleAtFixedRate(() -> {
            final List<StorageEngine> storageEngines = getShouldFlush();
            storageEngines.forEach(storageEngine -> FLUSH_POOL.submit(storageEngine::flush));
        }, 5, 5, TimeUnit.SECONDS);
    }


    private List<StorageEngine> getShouldFlush() {
        log.info("Start to check if any table needs to flush data...");
        return SlothSchemaHolder.INSTANCE.getSchemaMap().stream()
                .flatMap(schema -> schema.getAllTable().stream())
                .map(table -> ((SlothTable) table).getSlothTableEngine())
                .flatMap(tableEngine -> tableEngine.getStorageEngines().stream())
                .filter(StorageEngine::shouldFlush)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        //do nothing
    }
}
