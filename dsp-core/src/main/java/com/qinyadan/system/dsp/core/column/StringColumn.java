package com.qinyadan.system.dsp.core.column;

import com.qinyadan.system.dsp.core.block.Block;

public final class StringColumn extends AbstractColumn<String> {

    public StringColumn() {
        super(null, false);
    }

    @Override
    public void append(Object value) {
        // TODO: implement string column with variable-length blocks
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object get(int index) {
        return null;
    }

    @Override
    public boolean isNull(int index) {
        return true;
    }
}
