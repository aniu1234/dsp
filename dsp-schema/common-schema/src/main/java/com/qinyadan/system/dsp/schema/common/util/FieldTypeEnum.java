package com.qinyadan.system.dsp.schema.common.util;

import org.apache.calcite.linq4j.tree.Primitive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public enum FieldTypeEnum {
    /**
     *
     */
    STRING(null, String.class),

    /**
     *
     */
    BOOLEAN(Primitive.BOOLEAN),

    /**
     *
     */
    BYTE(Primitive.BYTE),
    /**
     *
     */
    CHAR(Primitive.CHAR),

    /**
     *
     */
    SHORT(Primitive.SHORT),

    /**
     *
     */
    INT(Primitive.INT),

    /**
     *
     */
    LONG(Primitive.LONG),

    /**
     *
     */
    FLOAT(Primitive.FLOAT),

    /**
     *
     */
    DOUBLE(Primitive.DOUBLE),

    /**
     *
     */
    DATE(null, java.sql.Date.class),

    /**
     *
     */
    TIME(null, java.sql.Time.class),

    /**
     *
     */
    TIMESTAMP(null, java.sql.Timestamp.class);

    private final Primitive primitive;
    private final Class clazz;

    private static final Map<String, FieldTypeEnum> MAP;


    FieldTypeEnum(Primitive primitive) {
        this(primitive, primitive.boxClass);
    }

    FieldTypeEnum(Primitive primitive, Class clazz) {
        this.primitive = primitive;
        this.clazz = clazz;
    }


    static {
        Map<String, FieldTypeEnum> builder = new HashMap<>();
        for (FieldTypeEnum value : values()) {
            builder.put(value.clazz.getSimpleName().toLowerCase(Locale.ROOT), value);

            if (value.primitive != null) {
                builder.put(value.primitive.primitiveName.toLowerCase(Locale.ROOT), value);
            }
        }
        MAP = Collections.unmodifiableMap(builder);
    }

    public static Class<?> getByTypeName(String type) {
        return getByType(type).clazz;
    }

    public static FieldTypeEnum getByType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Field type must not be null");
        }
        FieldTypeEnum fieldType = MAP.get(type.trim().toLowerCase(Locale.ROOT));
        if (fieldType == null) {
            throw new IllegalArgumentException("Unsupported field type '" + type + "'");
        }
        return fieldType;
    }

    public Class<?> getJavaClass() {
        return clazz;
    }
}
