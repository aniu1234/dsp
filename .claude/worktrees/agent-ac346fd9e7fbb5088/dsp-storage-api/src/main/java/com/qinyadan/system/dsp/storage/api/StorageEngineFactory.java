package com.qinyadan.system.dsp.storage.api;

public interface StorageEngineFactory {
    String type();
    StorageEngine create(EngineConfig config);
}
