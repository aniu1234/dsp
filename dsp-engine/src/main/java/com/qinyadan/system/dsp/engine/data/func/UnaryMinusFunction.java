package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;


public class UnaryMinusFunction extends Scalar {

    public UnaryMinusFunction() {
        super(1);
    }

    public static final UnaryMinusFunction INSTANCE = new UnaryMinusFunction();

    @Override
    public Value evaluate(List<Value> args, DataType dataType) {
        assert args.size() == 1;

        final Value value = args.get(0);

        final Value r = value.copy();
        if (DataTypes.INTEGER_TYPES.contains(dataType)) {
            r.setValue(-value.longValue());
        } else {
            r.setValue(-value.doubleValue());
        }

        return r;
    }
}
