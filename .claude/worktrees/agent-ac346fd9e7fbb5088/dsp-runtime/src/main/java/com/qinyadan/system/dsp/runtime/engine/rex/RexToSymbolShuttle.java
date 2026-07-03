package com.qinyadan.system.dsp.runtime.engine.rex;

import com.qinyadan.system.dsp.runtime.engine.data.expr.ColumnReference;
import com.qinyadan.system.dsp.runtime.engine.data.expr.Function;
import com.qinyadan.system.dsp.runtime.engine.data.expr.Literal;
import com.qinyadan.system.dsp.runtime.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.runtime.engine.data.func.Scalar;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import com.qinyadan.system.dsp.runtime.util.FunctionMappingUtils;
import com.qinyadan.system.dsp.runtime.util.TypeConversionUtils;
import org.apache.calcite.rex.*;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.NlsString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class RexToSymbolShuttle implements RexVisitor<Symbol> {

    public static final RexToSymbolShuttle INSTANCE = new RexToSymbolShuttle();

    @Override
    public Symbol visitInputRef(RexInputRef inputRef) {
        final SqlTypeName sqlTypeName = inputRef.getType().getSqlTypeName();
        DataType dataType = TypeConversionUtils.getBySqlTypeName(sqlTypeName);

        return new ColumnReference(dataType, inputRef.getIndex());
    }

    @Override
    public Symbol visitLocalRef(RexLocalRef localRef) {
        return null;
    }

    @Override
    public Symbol visitLiteral(RexLiteral literal) {
        final SqlTypeName sqlTypeName = literal.getType().getSqlTypeName();
        DataType dataType = TypeConversionUtils.getBySqlTypeName(sqlTypeName);

        //TODO why 1.5 literal.getValue2() return 15 ?????
        Object rawValue = literal.getValue();

        if (rawValue instanceof NlsString) {
            rawValue = ((NlsString) rawValue).getValue();
        }
        Value value = new Value(rawValue, dataType);
        return new Literal(value);
    }

    @Override
    public Symbol visitCall(RexCall call) {
        final String operatorName = call.getOperator().getName();
        Scalar scalar = FunctionMappingUtils.getFunctionByName(operatorName);

        if (Objects.isNull(scalar)) {
            throw new RuntimeException(String.format("Currently we do not support '%s' function", operatorName));
        }

        List<Symbol> ops = call.getOperands().stream()
                .map(operand -> operand.accept(this))
                .collect(Collectors.toList());

        final SqlTypeName sqlTypeName = call.getType().getSqlTypeName();
        DataType returnType = TypeConversionUtils.getBySqlTypeName(sqlTypeName);

        return new Function(returnType, ops, scalar);
    }

    @Override
    public Symbol visitOver(RexOver over) {
        return null;
    }

    @Override
    public Symbol visitCorrelVariable(RexCorrelVariable correlVariable) {
        return null;
    }

    @Override
    public Symbol visitDynamicParam(RexDynamicParam dynamicParam) {
        return null;
    }

    @Override
    public Symbol visitRangeRef(RexRangeRef rangeRef) {
        return null;
    }

    @Override
    public Symbol visitFieldAccess(RexFieldAccess fieldAccess) {
        return null;
    }

    @Override
    public Symbol visitSubQuery(RexSubQuery subQuery) {
        return null;
    }

    @Override
    public Symbol visitTableInputRef(RexTableInputRef fieldRef) {
        return null;
    }

    @Override
    public Symbol visitPatternFieldRef(RexPatternFieldRef fieldRef) {
        return null;
    }
}
