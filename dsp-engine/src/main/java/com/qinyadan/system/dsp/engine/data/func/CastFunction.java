package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public class CastFunction extends Scalar {

    public static final CastFunction INSTANCE = new CastFunction();

    public CastFunction() {
        super(1);
    }

    @Override
    protected Value doEvaluate(List<Value> args, DataType returnType) {

        final Value value = args.get(0);
        return new Value(value.getValue(), returnType);
    }
}
