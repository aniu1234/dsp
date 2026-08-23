package com.qinyadan.system.dsp.engine.service.dto;

public final class MetricsSnapshot {

    private final long uptimeMillis;
    private final long queriesSucceeded;
    private final long queriesFailed;
    private final long rowsReturned;
    private final long writesSucceeded;
    private final long writesFailed;
    private final long rowsWritten;
    private final long idempotentReplays;
    private final long mutationsSucceeded;
    private final long mutationsFailed;
    private final long rowsMutated;

    public MetricsSnapshot(long uptimeMillis, long queriesSucceeded,
                           long queriesFailed, long rowsReturned,
                           long writesSucceeded, long writesFailed,
                           long rowsWritten, long idempotentReplays,
                           long mutationsSucceeded, long mutationsFailed,
                           long rowsMutated) {
        this.uptimeMillis = uptimeMillis;
        this.queriesSucceeded = queriesSucceeded;
        this.queriesFailed = queriesFailed;
        this.rowsReturned = rowsReturned;
        this.writesSucceeded = writesSucceeded;
        this.writesFailed = writesFailed;
        this.rowsWritten = rowsWritten;
        this.idempotentReplays = idempotentReplays;
        this.mutationsSucceeded = mutationsSucceeded;
        this.mutationsFailed = mutationsFailed;
        this.rowsMutated = rowsMutated;
    }

    public long getUptimeMillis() { return uptimeMillis; }
    public long getQueriesSucceeded() { return queriesSucceeded; }
    public long getQueriesFailed() { return queriesFailed; }
    public long getRowsReturned() { return rowsReturned; }
    public long getWritesSucceeded() { return writesSucceeded; }
    public long getWritesFailed() { return writesFailed; }
    public long getRowsWritten() { return rowsWritten; }
    public long getIdempotentReplays() { return idempotentReplays; }
    public long getMutationsSucceeded() { return mutationsSucceeded; }
    public long getMutationsFailed() { return mutationsFailed; }
    public long getRowsMutated() { return rowsMutated; }
}
