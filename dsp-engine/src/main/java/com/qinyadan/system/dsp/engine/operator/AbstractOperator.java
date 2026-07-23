package com.qinyadan.system.dsp.engine.operator;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.util.TypeConversionUtils;
import org.apache.calcite.rel.type.RelDataType;

import java.util.List;
import java.util.stream.Collectors;


public abstract class AbstractOperator<R> implements Operator<R> {

    protected RelDataType rowTypes;

    public AbstractOperator(RelDataType rowTypes) {
        this.rowTypes = rowTypes;
    }

    @Override
    public List<DataType> getRowType() {
        return rowTypes.getFieldList().stream()
                .map(f -> f.getType().getSqlTypeName())
                .map(TypeConversionUtils::getBySqlTypeName)
                .collect(Collectors.toList());
    }
}
