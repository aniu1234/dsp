package com.qinyadan.system.dsp.schema.common.util;

import org.junit.Test;

import java.sql.Date;

import static org.junit.Assert.assertEquals;


public class TypeConversionUtilsTest {

    @Test
    public void convertsNumbersAndTemporalValuesStrictly() {
        assertEquals(12, TypeConversionUtils.toObject(FieldTypeEnum.INT, "12"));
        assertEquals(7L, TypeConversionUtils.toObject(FieldTypeEnum.LONG, 7));
        assertEquals(Date.valueOf("2026-07-22"),
                TypeConversionUtils.toObject(FieldTypeEnum.DATE, "2026-07-22"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidNumbersInsteadOfReturningNull() {
        TypeConversionUtils.toObject(FieldTypeEnum.INT, "not-a-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownFieldType() {
        FieldTypeEnum.getByType("money");
    }
}
