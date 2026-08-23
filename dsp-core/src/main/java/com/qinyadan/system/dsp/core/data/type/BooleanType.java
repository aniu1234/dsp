package com.qinyadan.system.dsp.core.data.type;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;


public class BooleanType extends DataType<Boolean> implements FixedWidthType {

    public static final int ID = 3;
    public static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {
    }

    private static final Map<String, Boolean> BOOLEAN_MAP = Maps.newHashMap();

    static {
        BOOLEAN_MAP.put("f", false);
        BOOLEAN_MAP.put("false", false);
        BOOLEAN_MAP.put("t", true);
        BOOLEAN_MAP.put("true", true);
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Precedence precedence() {
        return Precedence.BOOLEAN;
    }

    @Override
    public String getName() {
        return "boolean";
    }

    @Override
    public Boolean value(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return booleanFromString((String) value);
        }
        if (value instanceof Number) {
            return booleanFromNumber((Number) value);
        }
        return (Boolean) value;
    }


    private Boolean booleanFromString(String value) {
        String lowerValue = value.toLowerCase(Locale.ENGLISH);
        Boolean boolValue = BOOLEAN_MAP.get(lowerValue);
        if (boolValue == null) {
            throw new IllegalArgumentException("Can't convert \"" + value + "\" to boolean");
        } else {
            return boolValue;
        }
    }

    private Boolean booleanFromNumber(Number value) {
        return BigDecimal.ZERO.compareTo(new BigDecimal(value.toString())) != 0;
    }


    @Override
    public int fixedSize() {
        return 8;
    }

    @Override
    public Boolean readValueFrom(InputStream in) throws IOException {
        //todo;
        return null;
    }

    @Override
    public void writeValueTo(OutputStream out) throws IOException {
        //todo
    }

    @Override
    public int compare(Boolean o1, Boolean o2) {
        return Boolean.compare(o1, o2);
    }
}
