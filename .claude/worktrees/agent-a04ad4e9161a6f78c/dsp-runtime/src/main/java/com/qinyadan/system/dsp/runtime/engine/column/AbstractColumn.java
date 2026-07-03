package com.qinyadan.system.dsp.runtime.engine.column;


import com.qinyadan.system.dsp.runtime.engine.block.AbstractBlock;

import java.util.List;


public abstract class AbstractColumn<T> {

    protected List<AbstractBlock> blocks;
    /**
     * 默认值在插入的时候就补充好
     */
    protected T defaultValue;
    protected boolean notNull;
    /**
     * Start from 0;
     */
    protected int currentBlock;

    public AbstractColumn(List<AbstractBlock> blocks, T defaultValue, boolean notNull) {
        this.blocks = blocks;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
    }

    public abstract void put(T value);
}
