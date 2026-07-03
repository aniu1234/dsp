package com.qinyadan.system.dsp;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.runtime.LifeCycle;
import com.qinyadan.system.dsp.storage.StorageService;
import com.qinyadan.system.dsp.storage.parser.SlothSchemaHolder;
import com.qinyadan.system.dsp.storage.parser.util.SlothEnvironmentValueHolder;

import java.util.List;


public enum LifeCycleInstance {

    ;

    private static List<LifeCycle> lifeCycles = Lists.newArrayList();

    static {
        lifeCycles.add(SlothSchemaHolder.INSTANCE);
        lifeCycles.add(SlothEnvironmentValueHolder.INSTACNE);
        lifeCycles.add(StorageService.INSTANCE);
    }

    public static void add(LifeCycle lifeCycle) {
        lifeCycles.add(lifeCycle);
    }

    public static void start() {
        initAll();
        startAll();
    }

    public static void initAll() {
        lifeCycles.forEach(LifeCycle::init);
    }

    public static void startAll() {
        lifeCycles.forEach(LifeCycle::start);
    }

    public static void closeAll() {
        lifeCycles.forEach(LifeCycle::close);
    }
}
