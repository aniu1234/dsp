package com.qinyadan.system.dsp.runtime.engine.data.func.nil;

import com.qinyadan.system.dsp.runtime.engine.data.func.Scalar;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;

import java.util.List;

import static com.qinyadan.system.dsp.runtime.engine.data.type.DataTypes.BOOLEAN;


public class IsNullFunction extends Scalar {

    public static final IsNullFunction INSTANCE = new IsNullFunction();

    public IsNullFunction() {
        super(1);
    }

    @Override
    public Value evaluate(List<Value> args, DataType returnType) {
        final Value data = args.get(0);
        return new Value(data.isNull(), BOOLEAN);
    }
}

