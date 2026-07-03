package com.qinyadan.system.dsp.storage.parser.util;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.runtime.LifeCycle;
import com.qinyadan.system.dsp.storage.parser.EnvironmentValues;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


public class SlothEnvironmentValueHolder implements LifeCycle {

    public static final SlothEnvironmentValueHolder INSTACNE = new SlothEnvironmentValueHolder();

    @Getter
    @Setter
    private Map<String, String> properties;


    public void add(String key, String value) {
        properties.put(key, value);
    }

    public String propertyValue(String key) {
        return properties.get(key);
    }

    @Override
    public void init() {
        //load data from constants
        properties = Maps.newHashMap();
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
