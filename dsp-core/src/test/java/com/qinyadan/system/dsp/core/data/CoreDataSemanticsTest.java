package com.qinyadan.system.dsp.core.data;

import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CoreDataSemanticsTest {

    @Test
    public void numericBooleanUsesZeroAsFalse() {
        assertFalse(new Value(0, DataTypes.INTEGER).booleanValue());
        assertFalse(new Value(new BigDecimal("0.00"), DataTypes.DOUBLE).booleanValue());
        assertTrue(new Value(1, DataTypes.INTEGER).booleanValue());
        assertTrue(new Value(-1, DataTypes.INTEGER).booleanValue());
    }

    @Test
    public void numericValuesConvertToDeclaredDoubleType() {
        assertEquals(50D, new Value(50L, DataTypes.DOUBLE).doubleValue(), 0D);
    }

    @Test
    public void nullValuesHaveStableOrdering() {
        Value nullValue = Value.nullValue(DataTypes.INTEGER);
        Value value = new Value(1, DataTypes.INTEGER);

        assertEquals(0, nullValue.compareTo(Value.nullValue(DataTypes.INTEGER)));
        assertTrue(nullValue.compareTo(value) < 0);
        assertTrue(value.compareTo(nullValue) > 0);
    }

    @Test
    public void equalityUsesNormalizedValueAndType() {
        Value integerBackedLong = new Value(1, DataTypes.LONG);
        Value longBackedLong = new Value(1L, DataTypes.LONG);

        assertEquals(integerBackedLong, longBackedLong);
        assertEquals(integerBackedLong.hashCode(), longBackedLong.hashCode());
        assertNotEquals(Value.nullValue(DataTypes.INTEGER), Value.nullValue(DataTypes.LONG));
        assertNotEquals(integerBackedLong, null);
    }

    @Test
    public void rowDoesNotExposeItsBackingArray() {
        Value original = new Value(1, DataTypes.INTEGER);
        Value replacement = new Value(2, DataTypes.INTEGER);
        Value[] input = {original};
        Row row = RowImpl.of(input);

        input[0] = replacement;
        row.getColumns()[0] = replacement;

        assertEquals(original, row.getColumn(0));
    }

    @Test
    public void temporalValuesRoundTripWithStableUtcFormatting() {
        Value date = DataTypes.DATE.createByType("2026-07-20");
        Value timestamp = DataTypes.TIMESTAMP.createByType("2026-07-20 14:30:45");

        assertEquals("2026-07-20", date.stringValue());
        assertEquals("2026-07-20 14:30:45", timestamp.stringValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDatesAreRejectedInsteadOfBeingLenientlyNormalized() {
        DataTypes.DATE.createByType("2026-99-99");
    }
}
