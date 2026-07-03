package com.qinyadan.system.dsp.engine.data.value;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;


public class IntValue extends Value {

    public static final Value INT_NULL = new IntValue(null);

    public IntValue(Object value) {
        super(value);
    }

    @Override
    public DataType<?> getType() {
        return DataTypes.INTEGER;
    }

    @Override
    public Object getValue() {
        return intValue();
    }

    @Override
    public Integer intValue() {
        return (Integer) value;
    }

    @Override
    public Byte byteValue() {
        //check overflow
        return (Byte) value;
    }

    @Override
    public Short shortValue() {
        return (Short) value;
    }

    @Override
    public Long longValue() {
        return (Long) value;
    }

    @Override
    public Float floatValue() {
        return Float.valueOf(value.toString());
    }

    @Override
    public Double doubleValue() {
        return Double.valueOf(value.toString());
    }

    @Override
    public Boolean booleanValue() {
        return (Integer) value == 0;
    }

    @Override
    public String stringValue() {
        return value.toString();
    }

    @Override
    public IntValue copy() {
        return new IntValue(value);
    }
}
