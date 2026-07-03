package com.qinyadan.system.dsp.runtime.engine.column;

import com.qinyadan.system.dsp.runtime.engine.block.AbstractBlock;
import com.qinyadan.system.dsp.runtime.engine.block.BlockConstants;
import com.qinyadan.system.dsp.runtime.engine.block.IntBlock;

import java.util.List;


public class IntegerColumn extends AbstractColumn<Integer> {

    public IntegerColumn(List<AbstractBlock> blocks, Integer defaultValue, boolean notNull) {
        super(blocks, defaultValue, notNull);
        currentBlock = blocks.size() - 1;
    }

    @Override
    public void put(Integer value) {
        IntBlock block;

        if (currentBlock == -1) {
            block = new IntBlock(BlockConstants.BLOCK_SIZE);
            currentBlock++;
            blocks.add(block);
        } else {
            block = (IntBlock) blocks.get(currentBlock);
        }

        if (block.canAdd()) {
            block = new IntBlock(BlockConstants.BLOCK_SIZE);
            blocks.add(block);
            currentBlock++;
        }

        block.add(value);
    }

}
