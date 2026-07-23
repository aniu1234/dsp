package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public class MinAggregation extends MaxAggregation {

    public MinAggregation(boolean isDistinct, DataType inputType,
                          DataType resultType, int index) {
        super(isDistinct, inputType, resultType, index);
    }

    @Override
    protected Value select(List<Value> values) {
        return values.stream().min(Value::compareTo).get();
    }
}
