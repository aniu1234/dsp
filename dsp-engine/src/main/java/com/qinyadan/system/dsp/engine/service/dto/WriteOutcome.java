package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.storage.api.WriteResult;

public final class WriteOutcome {

    private final WriteResult result;
    private final boolean idempotentReplay;

    public WriteOutcome(WriteResult result, boolean idempotentReplay) {
        if (result == null) {
            throw new IllegalArgumentException("Write result is required");
        }
        this.result = result;
        this.idempotentReplay = idempotentReplay;
    }

    public WriteResult getResult() { return result; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
}
