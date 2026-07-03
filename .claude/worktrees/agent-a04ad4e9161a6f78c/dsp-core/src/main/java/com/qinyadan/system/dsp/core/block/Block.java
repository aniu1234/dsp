package com.qinyadan.system.dsp.core.block;

import java.nio.ByteBuffer;
import java.util.BitSet;

public abstract class Block {
    protected static final int DEFAULT_CAPACITY = 1 << 16;

    protected ByteBuffer buffer;
    protected BitSet nulls;
    protected int size;
    protected int currentSize;

    public Block(int size) {
        this.size = size;
        this.buffer = ByteBuffer.allocateDirect(size * 4);
        this.currentSize = 0;
        this.nulls = new BitSet(size);
    }

    public abstract boolean canAdd();
    public abstract void append(Object value);
    public abstract Object read(int index);
    public abstract boolean isNull(int index);

    public void free() {
        if (buffer != null && buffer.isDirect()) {
            buffer.clear();
        }
    }

    public int currentSize() { return currentSize; }
}
