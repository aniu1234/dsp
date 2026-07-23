package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

public class CountAggregation extends AbstractAggregation {
    private final boolean countAll;

    public CountAggregation(boolean isDistinct, DataType inputType, DataType resultType,
                            int index, boolean countAll) {
        super(isDistinct, inputType, resultType, index);
        this.countAll = countAll;
    }

    @Override
    public Value compute() {
        long count = countAll ? originDatas.size() : nonNullValues().size();
        return new Value(count, resultType);
    }
}
