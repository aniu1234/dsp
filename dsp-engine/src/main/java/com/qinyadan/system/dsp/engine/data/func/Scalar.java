package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;
import java.util.Objects;


public abstract class Scalar {

    private final int argumentCount;

    protected Scalar(int argumentCount) {
        this.argumentCount = argumentCount;
    }

    public final Value evaluate(List<Value> args, DataType returnType) {
        Objects.requireNonNull(args, "args");
        if (args.size() != argumentCount) {
            throw new IllegalArgumentException("Expected " + argumentCount
                    + " arguments but got " + args.size());
        }
        return doEvaluate(args, returnType);
    }

    protected abstract Value doEvaluate(List<Value> args, DataType returnType);
}
