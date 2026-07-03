package com.qinyadan.system.dsp.core.column;

import com.qinyadan.system.dsp.core.block.Block;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractColumn<T> implements Column {

    protected List<Block> blocks = new ArrayList<>();
    protected T defaultValue;
    protected boolean notNull;
    protected int currentBlock;

    public AbstractColumn(T defaultValue, boolean notNull) {
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.currentBlock = -1;
    }

    @Override
    public abstract void append(Object value);

    @Override
    public void flush() {
        // no-op
    }
}
