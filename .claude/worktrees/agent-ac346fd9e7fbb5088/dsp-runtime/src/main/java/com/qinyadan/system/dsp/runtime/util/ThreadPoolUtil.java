package com.qinyadan.system.dsp.runtime.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;


public class ThreadPoolUtil {

    public static final int CPUS = Runtime.getRuntime().availableProcessors() * 2;
    public static final ThreadPoolExecutor FLUSH_POOL;
    public static final ScheduledExecutorService SCHEDULE_POOL;

    static {
        FLUSH_POOL = new ThreadPoolExecutor(
                1,
                CPUS,
                30,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100),
                new ThreadFactoryBuilder().setNameFormat("flush_data_%s").build());

        FLUSH_POOL.allowCoreThreadTimeOut(true);
    }

    static {
        SCHEDULE_POOL = new ScheduledThreadPoolExecutor(
                CPUS,
                new ThreadFactoryBuilder().setNameFormat("scedule_flush_data_%s").build());
    }
}
