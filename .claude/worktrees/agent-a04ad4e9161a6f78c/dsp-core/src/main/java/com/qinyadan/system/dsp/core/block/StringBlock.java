package com.qinyadan.system.dsp.core.block;

public final class StringBlock extends Block {
    public StringBlock(int capacity) {
        super(capacity);
    }

    @Override
    public boolean canAdd() {
        return false;
    }

    @Override
    public void append(Object value) {
        // strings are variable-length, need separate block type
    }

    @Override
    public Object read(int index) {
        return null;
    }

    @Override
    public boolean isNull(int index) {
        return true;
    }
}
