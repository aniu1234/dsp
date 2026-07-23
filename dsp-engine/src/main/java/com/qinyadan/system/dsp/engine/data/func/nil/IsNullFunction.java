package com.qinyadan.system.dsp.engine.data.func.nil;

import com.qinyadan.system.dsp.engine.data.func.Scalar;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;

import static com.qinyadan.system.dsp.core.data.type.DataTypes.BOOLEAN;


public class IsNullFunction extends Scalar {

    public static final IsNullFunction INSTANCE = new IsNullFunction();

    public IsNullFunction() {
        super(1);
    }

    @Override
    protected Value doEvaluate(List<Value> args, DataType returnType) {
        final Value data = args.get(0);
        return new Value(data.isNull(), BOOLEAN);
    }
}

