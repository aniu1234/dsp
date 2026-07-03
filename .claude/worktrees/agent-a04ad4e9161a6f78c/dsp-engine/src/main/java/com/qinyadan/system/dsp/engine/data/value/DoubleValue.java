package com.qinyadan.system.dsp.engine.data.value;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;

import java.math.BigDecimal;


public class DoubleValue extends Value {

    public static final Value INT_NULL = new DoubleValue(null);


    public DoubleValue(Object value) {
        super(value);
    }

    @Override
    public DataType<?> getType() {
        return DataTypes.DOUBLE;
    }

    @Override
    public Integer intValue() {
        return Integer.valueOf(value.toString());
    }

    @Override
    public Byte byteValue() {
        return Byte.valueOf(value.toString());
    }

    @Override
    public Short shortValue() {
        return Short.valueOf(value.toString());
    }

    @Override
    public Long longValue() {
        return Long.valueOf(value.toString());
    }

    @Override
    public Float floatValue() {
        return Float.valueOf(value.toString());
    }

    @Override
    public Double doubleValue() {
        return (Double) value;
    }

    @Override
    public Boolean booleanValue() {
        BigDecimal bigDecimal = new BigDecimal(value.toString());
        return BigDecimal.ZERO.equals(bigDecimal);
    }

    @Override
    public String stringValue() {
        return value.toString();
    }

    @Override
    public Value copy() {
        return new DoubleValue(value);
    }
}
