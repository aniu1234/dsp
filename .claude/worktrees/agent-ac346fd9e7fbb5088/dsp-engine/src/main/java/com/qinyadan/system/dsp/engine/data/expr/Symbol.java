package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;


public abstract class Symbol implements FuncArg {

    protected List<Value> input;

    public Symbol(DataType<?> returnType) {
        this.returnType = returnType;
    }

    public void setInput(List<Value> input) {
        this.input = input;
    }

    public List<Value> getInput() {
        return input;
    }

    protected DataType<?> returnType;

    public static boolean isLiteral(Symbol symbol, DataType<?> expectedType) {
        return symbol.symbolType() == SymbolType.LITERAL && symbol.valueType().equals(expectedType);
    }

    public abstract SymbolType symbolType();

    public abstract Value compute();

    @Override
    public boolean canBeCasted() {
        return true;
    }

    @Override
    public DataType<?> valueType() {
        return returnType;
    }
}
