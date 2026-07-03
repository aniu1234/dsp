package com.qinyadan.system.dsp.core.data;

import com.qinyadan.system.dsp.core.data.value.Value;

public final class RowImpl implements Row {
    private final Value[] values;

    private RowImpl(Value[] values) {
        this.values = values;
    }

    public static RowImpl of(Value... values) {
        return new RowImpl(values);
    }

    public static RowImpl empty(int columnCount) {
        Value[] vals = new Value[columnCount];
        for (int i = 0; i < columnCount; i++) vals[i] = Value.nullValue(null);
        return new RowImpl(vals);
    }

    @Override
    public int columnSize() { return values.length; }

    @Override
    public Value getColumn(int i) { return values[i]; }

    @Override
    public Value[] getColumns() { return values; }
}
