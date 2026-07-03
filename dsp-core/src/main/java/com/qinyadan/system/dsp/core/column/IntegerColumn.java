package com.qinyadan.system.dsp.core.column;

import com.qinyadan.system.dsp.core.block.Block;
import com.qinyadan.system.dsp.core.block.IntBlock;

public final class IntegerColumn extends AbstractColumn<Integer> {

    public IntegerColumn() {
        super(0, false);
    }

    @Override
    public void append(Object value) {
        if (currentBlock == -1 || blocks.isEmpty()) {
            IntBlock block = new IntBlock(1 << 16);
            blocks.add(block);
            currentBlock = blocks.size() - 1;
        }

        Block block = blocks.get(currentBlock);
        if (!block.canAdd()) {
            IntBlock newBlock = new IntBlock(1 << 16);
            blocks.add(newBlock);
            currentBlock = blocks.size() - 1;
            block = newBlock;
        }

        block.append(value);
    }

    @Override
    public int size() {
        int total = 0;
        for (Block b : blocks) {
            total += b.currentSize();
        }
        return total;
    }

    @Override
    public Object get(int index) {
        int offset = 0;
        for (Block b : blocks) {
            if (index < offset + b.currentSize()) {
                return b.read(index - offset);
            }
            offset += b.currentSize();
        }
        return null;
    }

    @Override
    public boolean isNull(int index) {
        int offset = 0;
        for (Block b : blocks) {
            if (index < offset + b.currentSize()) {
                return b.isNull(index - offset);
            }
            offset += b.currentSize();
        }
        return true;
    }
}
