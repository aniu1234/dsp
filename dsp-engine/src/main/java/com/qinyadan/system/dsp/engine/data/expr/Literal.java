package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;


public class Literal extends Symbol implements Comparable<Literal> {

    private Value value;

    public Literal(Value value) {
        super(value.getType());
        this.value = value;
    }

    @Override
    public SymbolType symbolType() {
        return SymbolType.LITERAL;
    }

    @Override
    public DataType<?> valueType() {
        return value.getType();
    }

    @Override
    public int compareTo(Literal o) {
        return -1;
        //return value;
    }

    @Override
    public Value compute() {
        return value;
    }
}
