package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;
import java.util.stream.Collectors;


public class CountAggregation extends AbstractAggregation {
    private Value result;
    private long count;
    private boolean countStart;

    private List<Value> v;

    public CountAggregation(boolean isDistinct, boolean ignoreNull, DataType inputType, DataType resultType,
                            int index, boolean countStart, List<Integer> groupByIndex) {
        super(isDistinct, ignoreNull, inputType, resultType, index, groupByIndex);
        this.countStart = countStart;
    }

    @Override
    public void init() {
        result = new Value(0L, getResultType());
        count = 0;

        v = originDatas.stream()
                .map(values -> countStart ? values.getColumn(0) : values.getColumn(index))
                .collect(Collectors.toList());

        if (!countStart && isDistinct) {
            v = v.stream().distinct().collect(Collectors.toList());
        }
    }

    @Override
    public Value compute() {

        init();
        for (Value value : v) {
            if (countStart) {
                count++;
            } else if (!value.isNull()) {
                count++;
            }
        }
        result.setValue(count);
        return result;
    }
}
