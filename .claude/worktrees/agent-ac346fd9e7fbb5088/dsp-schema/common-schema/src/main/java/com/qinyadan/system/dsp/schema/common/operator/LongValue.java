package com.qinyadan.system.dsp.schema.common.operator;


public class LongValue implements Value<Long> {

    private Long value;

    @Override
    public Long getLong() {
        return null;
    }

    @Override
    public Integer getInt() {
        return null;
    }

    @Override
    public Long value() {
        return value;
    }
}
