package com.qinyadan.system.dsp.engine;

public interface LifeCycle {

    void init();


    default void start() {
    }

    void close();
}
