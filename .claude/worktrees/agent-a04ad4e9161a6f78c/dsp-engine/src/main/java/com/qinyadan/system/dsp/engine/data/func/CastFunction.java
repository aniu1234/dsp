package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;


public class CastFunction extends Scalar {

    public static final CastFunction INSTANCE = new CastFunction();

    public CastFunction() {
        super(1);
    }

    @Override
    public Value evaluate(List<Value> args, DataType returnType) {

        final Value v = args.get(0);
        final Value copy = v.copy();
        copy.setDataType(returnType);

        return copy;
    }
}
