package com.qinyadan.system.dsp.runtime.engine.data.expr;

import com.qinyadan.system.dsp.runtime.engine.data.value.Value;


public class InputColumn extends Symbol {
    //this is table scan
    private Value value;

    public InputColumn(Value value) {
        super(value.getType());
        this.value = value;
    }

    @Override
    public SymbolType symbolType() {
        return SymbolType.INPUT_COLUMN;
    }

    @Override
    public Value compute() {
        return value;
    }
}
