package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;



public class MaxAggregation extends AbstractAggregation {

    public MaxAggregation(boolean isDistinct, DataType inputType,
                          DataType resultType, int index) {
        super(isDistinct, inputType, resultType, index);
    }

    @Override
    public Value compute() {
        List<Value> values = nonNullValues();
        if (values.isEmpty()) {
            return Value.nullValue(resultType);
        }

        Value result = select(values);
        return new Value(result.getValue(), resultType);
    }

    protected Value select(List<Value> values) {
        return values.stream().max(Value::compareTo).get();
    }
}
