package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.service.dto.MetricsSnapshot;

import java.util.concurrent.atomic.LongAdder;

/**
 * Process-local counters for the primary query and write paths.
 */
public final class RuntimeMetrics {

    public static final RuntimeMetrics INSTANCE = new RuntimeMetrics();

    private final long startedAt = System.currentTimeMillis();
    private final LongAdder queriesSucceeded = new LongAdder();
    private final LongAdder queriesFailed = new LongAdder();
    private final LongAdder rowsReturned = new LongAdder();
    private final LongAdder writesSucceeded = new LongAdder();
    private final LongAdder writesFailed = new LongAdder();
    private final LongAdder rowsWritten = new LongAdder();
    private final LongAdder idempotentReplays = new LongAdder();

    private RuntimeMetrics() {
    }

    public void querySucceeded(long rows) {
        queriesSucceeded.increment();
        rowsReturned.add(rows);
    }

    public void queryFailed() {
        queriesFailed.increment();
    }

    public void writeSucceeded(long rows) {
        writesSucceeded.increment();
        rowsWritten.add(rows);
    }

    public void writeFailed() {
        writesFailed.increment();
    }

    public void idempotentReplay() {
        idempotentReplays.increment();
    }

    public MetricsSnapshot snapshot() {
        return new MetricsSnapshot(System.currentTimeMillis() - startedAt,
                queriesSucceeded.sum(), queriesFailed.sum(), rowsReturned.sum(),
                writesSucceeded.sum(), writesFailed.sum(), rowsWritten.sum(),
                idempotentReplays.sum());
    }
}
