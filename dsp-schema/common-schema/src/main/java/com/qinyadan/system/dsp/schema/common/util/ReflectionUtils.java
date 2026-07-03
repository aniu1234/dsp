package com.qinyadan.system.dsp.schema.common.util;

import java.lang.reflect.Field;

public class ReflectionUtils {
    public static Field getField(Class c, String fieldName) {
        try {
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
