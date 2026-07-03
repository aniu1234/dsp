package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;


public class ColumnReference extends Symbol {
    private int index;

    public ColumnReference(DataType<?> returnType, int index) {
        super(returnType);
        this.index = index;
    }

    @Override
    public SymbolType symbolType() {
        return SymbolType.REFERNCE;
    }

    @Override
    public Value compute() {
        return getInput().get(index);
    }
}
