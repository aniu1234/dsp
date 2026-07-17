package com.qinyadan.system.dsp.engine.data;

import com.qinyadan.system.dsp.engine.data.func.CompareFunction;
import com.qinyadan.system.dsp.engine.data.func.LogicalFunction;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.data.value.Value;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ValueSemanticsTest {

    @Test
    public void numericBooleanUsesZeroAsFalse() {
        assertFalse(new Value(0, DataTypes.INTEGER).booleanValue());
        assertTrue(new Value(1, DataTypes.INTEGER).booleanValue());
        assertTrue(new Value(-1, DataTypes.INTEGER).booleanValue());
    }

    @Test
    public void comparisonWithNullReturnsUnknown() {
        Value result = CompareFunction.EQUALS.evaluate(Arrays.asList(
                new Value(null, DataTypes.INTEGER),
                new Value(null, DataTypes.INTEGER)), DataTypes.BOOLEAN);

        assertNull(result.booleanValue());
    }

    @Test
    public void logicalOperatorsFollowSqlThreeValuedLogic() {
        Value unknown = new Value(null, DataTypes.BOOLEAN);
        Value truth = Value.ofBooleanTrue();
        Value falsity = Value.ofBooleanFalse();

        assertTrue(LogicalFunction.LOGICAL_OR.evaluate(
                Arrays.asList(unknown, truth), DataTypes.BOOLEAN).booleanValue());
        assertNull(LogicalFunction.LOGICAL_OR.evaluate(
                Arrays.asList(unknown, falsity), DataTypes.BOOLEAN).booleanValue());
        assertFalse(LogicalFunction.LOGICAL_AND.evaluate(
                Arrays.asList(unknown, falsity), DataTypes.BOOLEAN).booleanValue());
        assertNull(LogicalFunction.LOGICAL_AND.evaluate(
                Arrays.asList(unknown, truth), DataTypes.BOOLEAN).booleanValue());
    }

    @Test
    public void equalTypedValuesHaveEqualHashCodes() {
        Value integerBackedLong = new Value(1, DataTypes.LONG);
        Value longBackedLong = new Value(1L, DataTypes.LONG);

        assertEquals(integerBackedLong, longBackedLong);
        assertEquals(integerBackedLong.hashCode(), longBackedLong.hashCode());
    }
}
