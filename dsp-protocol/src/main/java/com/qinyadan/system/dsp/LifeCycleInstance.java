package com.qinyadan.system.dsp;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.StorageService;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.EnvironmentService;

import java.util.List;
import java.util.ListIterator;


public enum LifeCycleInstance {

    ;

    private static final List<LifeCycle> lifeCycles = Lists.newArrayList(
            CatalogService.INSTANCE,
            EnvironmentService.INSTANCE,
            StorageService.INSTANCE
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
        ListIterator<LifeCycle> iterator = lifeCycles.listIterator(lifeCycles.size());
        while (iterator.hasPrevious()) {
            iterator.previous().close();
        }
    }
}
