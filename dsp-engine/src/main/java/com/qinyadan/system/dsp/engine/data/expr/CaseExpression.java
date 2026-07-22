package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


/**
 * Lazy SQL CASE expression. Calcite represents searched CASE as alternating
 * condition/result operands followed by the ELSE operand.
 */
public class CaseExpression extends Symbol {

    private final List<Symbol> operands;

    public CaseExpression(DataType<?> returnType, List<Symbol> operands) {
        super(returnType);
        if (operands.size() < 3 || operands.size() % 2 == 0) {
            throw new IllegalArgumentException("CASE requires condition/result pairs and ELSE");
        }
        this.operands = operands;
    }

    @Override
    public SymbolType symbolType() {
        return SymbolType.FUNCTION;
    }

    @Override
    public Value compute() {
        for (int i = 0; i < operands.size() - 1; i += 2) {
            Value condition = operands.get(i).compute();
            if (!condition.isNull() && Boolean.TRUE.equals(condition.booleanValue())) {
                return coerce(operands.get(i + 1).compute());
            }
        }
        return coerce(operands.get(operands.size() - 1).compute());
    }

    private Value coerce(Value value) {
        return value.isNull() ? Value.nullValue(returnType)
                : returnType.createByType(value.getValue());
    }

    @Override
    public void setInput(List<Value> input) {
        this.input = input;
        for (Symbol operand : operands) {
            operand.setInput(input);
        }
    }
}
