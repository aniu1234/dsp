package com.qinyadan.system.dsp.storage.api;

import lombok.Value;

@Value
public class WriteResult {
    int inserted;
    long timestamp;

    public WriteResult(int inserted, long timestamp) {
        if (inserted < 0) {
            throw new IllegalArgumentException("Inserted row count must not be negative");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Write timestamp must not be negative");
        }
        this.inserted = inserted;
        this.timestamp = timestamp;
    }
}
