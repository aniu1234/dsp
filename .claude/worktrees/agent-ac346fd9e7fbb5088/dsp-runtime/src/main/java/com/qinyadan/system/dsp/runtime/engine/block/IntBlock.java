package com.qinyadan.system.dsp.runtime.engine.block;

import com.qinyadan.system.dsp.runtime.constant.TypeConstants;

import java.util.Objects;



public class IntBlock extends AbstractBlock<Integer> {

    public IntBlock(int size) {
        super(size * TypeConstants.INT_SIZE);
    }

    @Override
    public boolean canAdd() {
        return currentSize + 1 <= BlockConstants.BLOCK_SIZE;
    }

    @Override
    public void add(Integer v) {
        if (Objects.isNull(v)) {
            nullBitSet.set(currentSize++);
            return;
        }

        UNSAFE.putInt(base + currentSize * TypeConstants.INT_SIZE, v);
        currentSize++;
    }

    @Override
    public Integer read(int pos) {
        if (nullBitSet.get(pos)) {
            return null;
        }

        return UNSAFE.getInt(base + pos * TypeConstants.INT_SIZE);
    }
}
