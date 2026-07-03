package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.Maps;

import java.util.Map;


public class EnvironmentValues {
    public static final Map<String, String> GLOBAL_ENVIRONMENT = Maps.newHashMap();

    static {
        GLOBAL_ENVIRONMENT.put("version_comment", "Create by DSP");
    }
}
