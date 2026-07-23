package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public class UnaryMinusFunction extends Scalar {

    public UnaryMinusFunction() {
        super(1);
    }

    public static final UnaryMinusFunction INSTANCE = new UnaryMinusFunction();

    @Override
    protected Value doEvaluate(List<Value> args, DataType dataType) {
        final Value value = args.get(0);
        if (value.isNull()) {
            return Value.nullValue(dataType);
        }

        if (DataTypes.INTEGER_TYPES.contains(dataType)) {
            return new Value(-value.longValue(), dataType);
        }
        return new Value(-value.doubleValue(), dataType);
    }
}
