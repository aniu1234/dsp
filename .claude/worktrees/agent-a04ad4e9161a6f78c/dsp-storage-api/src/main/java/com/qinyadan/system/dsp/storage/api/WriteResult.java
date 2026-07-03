package com.qinyadan.system.dsp.storage.api;

import lombok.Value;

@Value
public class WriteResult {
    int inserted;
    long timestamp;
}
