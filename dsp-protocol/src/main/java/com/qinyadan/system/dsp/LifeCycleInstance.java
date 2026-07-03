package com.qinyadan.system.dsp;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.StorageService;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.EnvironmentValueHolder;

import java.util.List;


public enum LifeCycleInstance {

    ;

    @SuppressWarnings("unchecked")
    private static final List<LifeCycle> lifeCycles = Lists.newArrayList(
            SlothSchemaHolder.INSTANCE,
            (LifeCycle) (Object) EnvironmentValueHolder.INSTACNE,
            (LifeCycle) (Object) StorageService.INSTANCE
    );

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
