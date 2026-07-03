package com.qinyadan.system.dsp.runtime;

public interface LifeCycle {

    void init();


    default void start() {
    }

    void close();
}
