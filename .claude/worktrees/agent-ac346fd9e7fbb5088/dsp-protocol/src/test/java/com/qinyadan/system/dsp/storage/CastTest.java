package com.qinyadan.system.dsp.storage;

import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import org.junit.Test;

import java.math.BigDecimal;

public class CastTest {

    @Test
    public void testCast() {
        BigDecimal l = new BigDecimal(50.0);
        long r = l.unscaledValue().longValue();
        long d = Long.class.cast(r);

        Value value =  new Value(d, DataTypes.DOUBLE);
        System.out.println(value.doubleValue());
    }
}
