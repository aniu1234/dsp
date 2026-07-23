package com.qinyadan.system.dsp.engine.rex;

import com.qinyadan.system.dsp.engine.data.expr.ColumnReference;
import com.qinyadan.system.dsp.engine.data.expr.CaseExpression;
import com.qinyadan.system.dsp.engine.data.expr.Function;
import com.qinyadan.system.dsp.engine.data.expr.Literal;
import com.qinyadan.system.dsp.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.engine.data.func.Scalar;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.util.FunctionMappingUtils;
import com.qinyadan.system.dsp.core.util.TypeConversionUtils;
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
        throw unsupported("local reference", localRef);
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
        final SqlTypeName sqlTypeName = call.getType().getSqlTypeName();
        DataType returnType = TypeConversionUtils.getBySqlTypeName(sqlTypeName);
        List<Symbol> ops = call.getOperands().stream()
                .map(operand -> operand.accept(this))
                .collect(Collectors.toList());

        if (call.getKind() == org.apache.calcite.sql.SqlKind.CASE) {
            return new CaseExpression(returnType, ops);
        }
        Scalar scalar = FunctionMappingUtils.getFunctionByName(operatorName);

        if (Objects.isNull(scalar)) {
            throw new RuntimeException(String.format("Currently we do not support '%s' function", operatorName));
        }

        return new Function(returnType, ops, scalar);
    }

    @Override
    public Symbol visitOver(RexOver over) {
        throw unsupported("window expression", over);
    }

    @Override
    public Symbol visitCorrelVariable(RexCorrelVariable correlVariable) {
        throw unsupported("correlated variable", correlVariable);
    }

    @Override
    public Symbol visitDynamicParam(RexDynamicParam dynamicParam) {
        throw unsupported("dynamic parameter", dynamicParam);
    }

    @Override
    public Symbol visitRangeRef(RexRangeRef rangeRef) {
        throw unsupported("range reference", rangeRef);
    }

    @Override
    public Symbol visitFieldAccess(RexFieldAccess fieldAccess) {
        throw unsupported("field access", fieldAccess);
    }

    @Override
    public Symbol visitSubQuery(RexSubQuery subQuery) {
        throw unsupported("subquery", subQuery);
    }

    @Override
    public Symbol visitTableInputRef(RexTableInputRef fieldRef) {
        throw unsupported("table input reference", fieldRef);
    }

    @Override
    public Symbol visitPatternFieldRef(RexPatternFieldRef fieldRef) {
        throw unsupported("pattern field reference", fieldRef);
    }

    private UnsupportedOperationException unsupported(String feature, RexNode node) {
        return new UnsupportedOperationException("Unsupported SQL expression " + feature + ": " + node);
    }
}
