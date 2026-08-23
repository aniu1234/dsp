package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.calcite.EnvironmentValueHolder;

/**
 * Application-facing access to global SQL environment values.
 */
public final class EnvironmentService implements LifeCycle {

    public static final EnvironmentService INSTANCE = new EnvironmentService();

    private final EnvironmentValueHolder values;

    private EnvironmentService() {
        this.values = EnvironmentValueHolder.INSTACNE;
    }

    public void setGlobal(String key, String value) {
        values.add(key, value);
    }

    public String getGlobal(String key) {
        return values.propertyValue(key);
    }

    @Override
    public void init() {
        values.init();
    }

    @Override
    public void start() {
        values.start();
    }

    @Override
    public void close() {
        values.close();
    }
}
