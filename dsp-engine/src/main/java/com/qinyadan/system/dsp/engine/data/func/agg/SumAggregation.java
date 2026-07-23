package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public class SumAggregation extends AbstractAggregation {

    public SumAggregation(boolean isDistinct, DataType inputType,
                          DataType resultType, int index) {
        super(isDistinct, inputType, resultType, index);
    }

    @Override
    public Value compute() {
        List<Value> values = nonNullValues();
        if (values.isEmpty()) {
            return Value.nullValue(resultType);
        }

        if (DataTypes.DECIMAL_TYPES.contains(inputType)) {
            double result = 0.0;
            for (Value value : values) {
                result += value.doubleValue();
            }
            return new Value(result, resultType);
        }

        long result = 0L;
        for (Value value : values) {
            result += value.longValue();
        }
        return new Value(result, resultType);
    }
}
