package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;
import java.util.stream.Collectors;


public class MinAggregation extends MaxAggregation {

    public MinAggregation(boolean isDistinct, boolean ignoreNull, DataType inputType,
                          DataType resultType, int index, List<Integer> groupByIndex) {
        super(isDistinct, ignoreNull, inputType, resultType, index, groupByIndex);
    }

    @Override
    public Value compute() {
        init();
        if (v.isEmpty()) {
            r.setValue(null);
        } else {
            Value min = v.stream().min(Value::compareTo).get();
            Value n = min.copy();
            n.setDataType(r.getType());
            r = n;
        }

        return r;
    }

    @Override
    public void init() {
        r = new Value(null, resultType);

        v = originDatas.stream()
                .map(values -> values.getColumn(index))
                .filter(value -> !value.isNull())
                .collect(Collectors.toList());

        if (isDistinct) {
            v = v.stream().distinct().collect(Collectors.toList());
        }
    }
}
