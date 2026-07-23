package com.qinyadan.system.dsp.core.data.type;

import com.qinyadan.system.dsp.core.util.TimeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DateType extends DataType<Long> implements FixedWidthType {

    public static final DateType INSTANCE = new DateType();

    public static final int ID = 12;

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Precedence precedence() {
        return Precedence.DATE;
    }

    @Override
    public String getName() {
        return "date";
    }

    @Override
    public Long value(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            Long parsed = TimeUtils.getDate((String) value);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid date value: " + value);
            }
            return parsed;
        }
        throw new IllegalArgumentException("Cannot convert value to date: " + value);
    }

    @Override
    public int fixedSize() {
        return Long.SIZE;
    }

    @Override
    public Long readValueFrom(InputStream in) throws IOException {
        return null;
    }

    @Override
    public void writeValueTo(OutputStream out) throws IOException {

    }

    @Override
    public int compare(Long o1, Long o2) {
        return o1.compareTo(o2);
    }
}
