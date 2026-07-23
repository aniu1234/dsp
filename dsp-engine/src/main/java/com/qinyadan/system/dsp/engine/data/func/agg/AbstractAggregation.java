package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;
import java.util.stream.Collectors;


public abstract class AbstractAggregation implements Aggregation {

    protected final boolean isDistinct;

    protected final DataType inputType;
    protected final DataType resultType;

    protected final int index;
    protected List<SlothRow> originDatas;

    public AbstractAggregation(boolean isDistinct, DataType inputType,
                               DataType resultType, int index) {
        this.isDistinct = isDistinct;
        this.inputType = inputType;
        this.resultType = resultType;
        this.index = index;
    }

    @Override
    public DataType getResultType() {
        return resultType;
    }

    public void setOriginDatas(List<SlothRow> originDatas) {
        this.originDatas = originDatas;
    }

    protected List<Value> nonNullValues() {
        List<Value> values = originDatas.stream()
                .map(row -> row.getColumn(index))
                .filter(value -> !value.isNull())
                .collect(Collectors.toList());

        return isDistinct
                ? values.stream().distinct().collect(Collectors.toList())
                : values;
    }
}
