package com.qinyadan.system.dsp.raft.storage;


public interface StorageBackend {

    default void store() {
        return;
    }

    default void query() {
        return;
    }
}
