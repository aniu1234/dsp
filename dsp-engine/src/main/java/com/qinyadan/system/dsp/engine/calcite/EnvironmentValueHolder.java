package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.calcite.EnvironmentValues;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


public class EnvironmentValueHolder implements LifeCycle {

    public static final EnvironmentValueHolder INSTACNE = new EnvironmentValueHolder();

    @Getter
    @Setter
    private Map<String, String> properties;


    public void add(String key, String value) {
        properties.put(key.toLowerCase(Locale.ROOT), value);
    }

    public String propertyValue(String key) {
        return properties.get(key.toLowerCase(Locale.ROOT));
    }

    @Override
    public void init() {
        //load data from constants
        properties = new ConcurrentHashMap<>();
        properties.putAll(EnvironmentValues.GLOBAL_ENVIRONMENT);
    }

    @Override
    public void start() {
        //Currently do nothing, try to load data from disk
        //TODO store enviroment values in database
    }

    @Override
    public void close() {

    }
}
