package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.engine.data.func.Scalar;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;
import java.util.stream.Collectors;


public class Function extends Symbol {
    private List<Symbol> args;
    private Scalar operator;


    public Function(DataType<?> returnType, List<Symbol> args, Scalar operator) {
        super(returnType);
        this.args = args;
        this.operator = operator;
    }

    @Override
    public SymbolType symbolType() {
        return SymbolType.FUNCTION;
    }

    @Override
    public Value compute() {
        final List<Value> values = args.stream()
                .map(Symbol::compute)
                .collect(Collectors.toList());

        return operator.evaluate(values, returnType);
    }

    @Override
    public void setInput(List<Value> input) {
        this.input = input;
        args.forEach(arg -> arg.setInput(input));
    }
}
