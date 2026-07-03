package com.qinyadan.system.dsp.runtime.engine.block;


public class StringBlock extends AbstractBlock<String> {

    //由于string 是变长的, 需要独立搞一个适应string 类型的block
    public StringBlock(int size) {
        super(size);
    }

    @Override
    public boolean canAdd() {
        return false;
    }

    @Override
    public void add(String v) {

    }

    @Override
    public String read(int index) {
        return null;
    }
}
