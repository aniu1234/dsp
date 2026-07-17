package com.qinyadan.system.dsp.engine.data.value;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Objects;


public class Value implements Comparable<Value> {

    protected Object value;

    //最终的控制输出格式是由dataType确定的，可能value与DataType不一致
    //比如说 value = Integer, DataType为Long, 最终需要
    protected DataType dataType;

    private boolean mappinngNull = false;

    public Value(Object value) {
        this.value = value;
    }

    public Value(Object value, DataType dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    public Value(Object value, DataType dataType, boolean mappinngNull) {
        this.value = value;
        this.dataType = dataType;
        this.mappinngNull = mappinngNull;
    }

    public DataType<?> getType() {
        return dataType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isNull() {
        return null == value;
    }

    public Integer intValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Integer.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? 1 : 0;
        }

        if (cl == String.class) {
            return 0;
        }

        return 0;
    }

    public Byte byteValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Byte.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? (byte) 1 : (byte) 0;
        }

        if (cl == String.class) {
            return (byte) 0;
        }

        return (byte) 0;
    }

    public Short shortValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Short.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? (short) 1 : (short) 0;
        }

        if (cl == String.class) {
            return (short) 0;
        }

        return (short) 0;
    }

    public Long longValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Long.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? 1L : 0L;
        }

        if (cl == String.class) {
            return 0L;
        }

        return 0L;
    }

    public Float floatValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Float.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? 1f : 0f;
        }

        if (cl == String.class) {
            return 0f;
        }

        return 0f;
    }

    public Double doubleValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (value instanceof Number) {
            //TODO 溢出判断
            return Double.valueOf(value.toString());
        }

        if (cl == Boolean.class) {
            return ((Boolean) value) ? (double) 1 : (double) 0;
        }

        if (cl == String.class) {
            return (double) 0;
        }

        return (double) 0;
    }

    public Boolean booleanValue() {

        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (cl == Boolean.class) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return BigDecimal.ZERO.compareTo(new BigDecimal(value.toString())) != 0;
        }

        if (cl == String.class) {
            return false;
        }

        return false;
    }

    public Date dateValue() {
        if (isNull()) {
            return null;
        }

        final Class cl = value.getClass();

        if (cl == Long.class) {
            return new Date((long) value);
        }

        if (cl == String.class) {
            final Long t = TimeUtils.getDate((String) value);
            return t == null ? null : new Date(t);
        }

        return null;
    }

    public String stringValue() {

        if (isNull()) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        if (getType() == DataTypes.DATE) {
            Date date = dateValue();
            return TimeUtils.formatDate(date);
        }

        return value.toString();
    }

    public Value copy() {
        return new Value(value, dataType);
    }

    public Object getValueByType() {
        DataType dataType = getType();

        if (null == value) {
            return value;
        }

        if (dataType == DataTypes.INTEGER) {
            return intValue();
        } else if (dataType == DataTypes.BYTE) {
            return byteValue();
        } else if (dataType == DataTypes.SHORT) {
            return shortValue();
        } else if (dataType == DataTypes.LONG) {
            return longValue();
        } else if (dataType == DataTypes.DOUBLE) {
            return doubleValue();
        } else if (dataType == DataTypes.FLOAT) {
            return floatValue();
        } else if (dataType == DataTypes.BOOLEAN) {
            return booleanValue();
        } else if (dataType == DataTypes.DATE) {
            return dateValue();
        } else {
            return stringValue();
        }
    }

    @Override
    public int compareTo(Value o) {
        if (isNull()) {
            return o.isNull() ? 0 : -1;
        }
        if (o.isNull()) {
            return 1;
        }

        final DataType thisType = this.getType();
        final DataType thatType = o.dataType;

        //TODO 暂时不考虑boolean类型
        if (DataTypes.INTEGER_TYPES.contains(thisType) && DataTypes.INTEGER_TYPES.contains(thatType)) {
            return Long.compare(longValue(), o.longValue());
        }

        if (DataTypes.DATE_TIME_TYPES.contains(thisType) || DataTypes.DATE_TIME_TYPES.contains(thatType)) {
            return this.stringValue().compareTo(o.stringValue());
        }


        if (DataTypes.STRING == o.dataType && DataTypes.STRING == this.dataType) {
            return this.stringValue().compareTo(o.stringValue());
        }


        return BigDecimal.valueOf(doubleValue()).compareTo(BigDecimal.valueOf(o.doubleValue()));
    }

    public Value compare(Value o) {
        return null;
    }

    public static Value ofBooleanTrue() {
        return new Value(true, DataTypes.BOOLEAN);
    }

    public static Value ofBooleanFalse() {
        return new Value(false, DataTypes.BOOLEAN);
    }

    public static Value ofBooean(boolean b) {
        return new Value(b, DataTypes.BOOLEAN);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Value)) {
            return false;
        }

        Value v = (Value) o;
        if (!Objects.equals(dataType, v.dataType)) {
            return false;
        }
        return Objects.equals(this.getValueByType(), v.getValueByType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValueByType(), dataType);
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
}
