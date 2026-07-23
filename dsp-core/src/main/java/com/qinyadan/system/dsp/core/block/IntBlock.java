package com.qinyadan.system.dsp.core.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class IntBlock extends Block {
    public IntBlock(int capacity) {
        super(capacity);
        this.buffer = ByteBuffer.allocateDirect(capacity * 4);
        this.buffer.order(ByteOrder.nativeOrder());
    }

    @Override
    public boolean canAdd() {
        return currentSize + 1 <= size;
    }

    @Override
    public void append(Object value) {
        if (value == null) {
            nulls.set(currentSize);
        } else {
            nulls.clear(currentSize);
            buffer.putInt(currentSize * 4, (Integer) value);
        }
        currentSize++;
    }

    @Override
    public Object read(int index) {
        if (isNull(index)) return null;
        buffer.position(index * 4);
        return buffer.getInt();
    }

    @Override
    public boolean isNull(int index) {
        return nulls.get(index);
    }
}
