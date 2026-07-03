package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;

import java.lang.reflect.ParameterizedType;



public interface Handler<T> {

    void handle(ConnectionContext connectionContext, T type);

    /**
     * Get Class of T
     *
     * @return
     */
    default Class<T> getType() {
        Class<T> tClass = (Class<T>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        return tClass;
    }
}
