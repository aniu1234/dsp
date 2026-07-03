package com.qinyadan.system.dsp.schema.common.operator;


public class IntegerValue implements Value<Integer> {
    private Integer value;

    public IntegerValue(Integer value) {
        this.value = value;
    }

    @Override
    public Long getLong() {
        return null;
    }

    @Override
    public Integer getInt() {
        return null;
    }

    @Override
    public Integer value() {
        return value;
    }
}
