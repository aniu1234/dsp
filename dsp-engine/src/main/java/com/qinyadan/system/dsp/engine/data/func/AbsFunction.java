package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public class AbsFunction extends Scalar {

    //TODO 当前先做成单例，后面看看是否要用反射
    public static final AbsFunction INSTANCE = new AbsFunction();

    public AbsFunction() {
        super(1);
    }

    @Override
    protected Value doEvaluate(List<Value> args, DataType dataType) {
        final Value value = args.get(0);
        if (value.isNull()) {
            return Value.nullValue(dataType);
        }

        if (DataTypes.INTEGER_TYPES.contains(dataType)) {
            return new Value(Math.abs(value.longValue()), dataType);
        }
        return new Value(Math.abs(value.doubleValue()), dataType);
    }
}
