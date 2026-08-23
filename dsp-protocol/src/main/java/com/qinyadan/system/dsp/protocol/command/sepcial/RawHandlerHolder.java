package com.qinyadan.system.dsp.protocol.command.sepcial;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.constant.StringConstants;
import com.qinyadan.system.dsp.protocol.command.sqlnode.Handler;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;


public class RawHandlerHolder {

    public static final Map<String, Handler> RAW_HANDLER = Maps.newHashMap();

    public static final String RAW_COMMAND_EXPLAIN = "EXPLAIN";
    public static final String RAW_COMMAND_HEALTH = "SHOW DSP HEALTH";
    public static final String RAW_COMMAND_METRICS = "SHOW DSP METRICS";

    static {
        RAW_HANDLER.put(RAW_COMMAND_EXPLAIN, ExplainHandler.INSTANCE);
        RAW_HANDLER.put(RAW_COMMAND_HEALTH, OperationalStatusHandler.INSTANCE);
        RAW_HANDLER.put(RAW_COMMAND_METRICS, OperationalStatusHandler.INSTANCE);
    }


    public static Handler getRawHandler(String query) {

        final String q = query.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);

        if (Objects.equals(RAW_COMMAND_HEALTH, q)
                || Objects.equals(RAW_COMMAND_METRICS, q)) {
            return RAW_HANDLER.get(q);
        }

        final String firstWord = q.split(StringConstants.SPACE, 2)[0].trim();
        if (Objects.equals(RawHandlerHolder.RAW_COMMAND_EXPLAIN, firstWord)) {
            return RAW_HANDLER.get(RAW_COMMAND_EXPLAIN);
        }

        return null;
    }
}
