package com.qinyadan.system.dsp.runtime.engine.column;

import com.qinyadan.system.dsp.runtime.engine.block.AbstractBlock;

import java.util.List;


public class StringColumn extends AbstractColumn<String> {

    public StringColumn(List<AbstractBlock> blocks, String defaultValue, boolean notNull) {
        super(blocks, defaultValue, notNull);
    }

    @Override
    public void put(String value) {
        //todo
    }
}
