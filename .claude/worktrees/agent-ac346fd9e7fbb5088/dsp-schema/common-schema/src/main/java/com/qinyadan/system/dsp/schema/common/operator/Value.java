package com.qinyadan.system.dsp.schema.common.operator;


public interface Value<T> {
    /**
     * @return
     */
    Long getLong();


    /**
     * @return
     */
    Integer getInt();


    T value();
}
