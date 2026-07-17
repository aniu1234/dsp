package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;
import java.util.stream.Collectors;


public class SumAggregation extends AbstractAggregation {

    private Value r;
    private List<Value> v;

    public SumAggregation(boolean isDistinct, boolean ignoreNull, DataType inputType,
                          DataType resultType, int index, List<Integer> groupByIndex) {
        super(isDistinct, ignoreNull, inputType, resultType, index, groupByIndex);
    }

    @Override
    public Value compute() {
        init();
        if (v.isEmpty()) {
            r.setValue(null);
            return r;
        }

        if (DataTypes.DECIMAL_TYPES.contains(inputType)) {
            Double t = 0.0;
            for (Value value : v) {
                t += value.doubleValue();
            }

            r.setValue(t);
        } else {
            Long t = 0L;
            for (Value value : v) {
                t += value.longValue();
            }

            r.setValue(t);
        }

        return r;
    }

    @Override
    public void init() {
        if (DataTypes.DECIMAL_TYPES.contains(inputType)) {
            r = new Value(0.0, resultType);
        } else {
            r = new Value(0L, resultType);
        }

        v = originDatas.stream()
                .map(values -> values.getColumn(index))
                .filter(value -> !value.isNull())
                .collect(Collectors.toList());

        if (isDistinct) {
            v = v.stream().distinct().collect(Collectors.toList());
        }
    }
}
