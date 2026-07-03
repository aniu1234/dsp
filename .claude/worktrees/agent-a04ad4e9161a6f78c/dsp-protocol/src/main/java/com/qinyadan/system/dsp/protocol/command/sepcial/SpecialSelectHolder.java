package com.qinyadan.system.dsp.protocol.command.sepcial;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.protocol.command.sqlnode.Handler;

import java.util.Map;


public class SpecialSelectHolder {
    public static final Map<String, Handler> SPECIAL_HANDLER = Maps.newHashMap();

    static {
        SPECIAL_HANDLER.put("SELECT `DATABASE`()", CurrentDatabaseHandler.INSTANCE);
    }
}
