package com.qinyadan.system.dsp.storage.api.service;

import com.qinyadan.system.dsp.storage.api.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic flushes on registered storage engines.
 * A default implementation is provided; subclasses can customize
 * the flush interval and engine selection.
 */
public class FlushScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(FlushScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<StorageEngine> engines;
    private final long intervalMs;

    public FlushScheduler(List<StorageEngine> engines, long intervalMs) {
        this.engines = engines;
        this.intervalMs = intervalMs;
    }

    /**
     * Start periodic flush scheduling.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            for (StorageEngine engine : engines) {
                if (engine.shouldFlush()) {
                    try {
                        engine.flush();
                    } catch (Exception e) {
                        LOG.warn("Flush failed for engine: {}", engine.getClass().getSimpleName(), e);
                    }
                }
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        LOG.info("FlushScheduler started with interval={}ms, engines={}", intervalMs, engines.size());
    }

    /**
     * Shut down the scheduler and force a final flush.
     */
    public void stop() {
        for (StorageEngine engine : engines) {
            try {
                engine.flush();
            } catch (Exception e) {
                LOG.warn("Final flush failed for engine: {}", engine.getClass().getSimpleName(), e);
            }
        }
        scheduler.shutdown();
    }
}
