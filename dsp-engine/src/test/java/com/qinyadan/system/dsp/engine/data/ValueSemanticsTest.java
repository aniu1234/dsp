package com.qinyadan.system.dsp.engine.data;

import com.qinyadan.system.dsp.engine.data.func.AbsFunction;
import com.qinyadan.system.dsp.engine.data.func.ArithmeticFunction;
import com.qinyadan.system.dsp.engine.data.func.CompareFunction;
import com.qinyadan.system.dsp.engine.data.func.LogicalFunction;
import com.qinyadan.system.dsp.engine.data.expr.CaseExpression;
import com.qinyadan.system.dsp.engine.data.expr.Literal;
import com.qinyadan.system.dsp.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.engine.data.expr.SymbolType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ValueSemanticsTest {

    @Test
    public void comparisonWithNullReturnsUnknown() {
        Value result = CompareFunction.EQUALS.evaluate(Arrays.asList(
                new Value(null, DataTypes.INTEGER),
                new Value(null, DataTypes.INTEGER)), DataTypes.BOOLEAN);

        assertNull(result.booleanValue());
    }

    @Test
    public void logicalOperatorsFollowSqlThreeValuedLogic() {
        Value unknown = new Value(null, DataTypes.BOOLEAN);
        Value truth = Value.ofBooleanTrue();
        Value falsity = Value.ofBooleanFalse();

        assertTrue(LogicalFunction.LOGICAL_OR.evaluate(
                Arrays.asList(unknown, truth), DataTypes.BOOLEAN).booleanValue());
        assertNull(LogicalFunction.LOGICAL_OR.evaluate(
                Arrays.asList(unknown, falsity), DataTypes.BOOLEAN).booleanValue());
        assertFalse(LogicalFunction.LOGICAL_AND.evaluate(
                Arrays.asList(unknown, falsity), DataTypes.BOOLEAN).booleanValue());
        assertNull(LogicalFunction.LOGICAL_AND.evaluate(
                Arrays.asList(unknown, truth), DataTypes.BOOLEAN).booleanValue());
    }

    @Test
    public void arithmeticWithNullReturnsUnknown() {
        Value result = ArithmeticFunction.PLUS.evaluate(Arrays.asList(
                Value.nullValue(DataTypes.INTEGER),
                new Value(1, DataTypes.INTEGER)), DataTypes.INTEGER);

        assertTrue(result.isNull());
    }

    @Test(expected = IllegalArgumentException.class)
    public void scalarFunctionsValidateArgumentCount() {
        AbsFunction.INSTANCE.evaluate(Collections.emptyList(), DataTypes.INTEGER);
    }

    @Test
    public void rowsWithEqualValuesHaveValueSemantics() {
        SlothRow first = new SlothRow(Arrays.asList(
                new Value(1, DataTypes.INTEGER),
                new Value("dsp", DataTypes.STRING)));
        SlothRow second = new SlothRow(Arrays.asList(
                new Value(1, DataTypes.INTEGER),
                new Value("dsp", DataTypes.STRING)));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(1L, Arrays.asList(first, second).stream().distinct().count());
    }

    @Test
    public void caseExpressionOnlyEvaluatesTheSelectedBranch() {
        Symbol unexpectedElse = new Symbol(DataTypes.INTEGER) {
            @Override
            public SymbolType symbolType() {
                return SymbolType.FUNCTION;
            }

            @Override
            public Value compute() {
                throw new AssertionError("unselected CASE branch was evaluated");
            }
        };
        CaseExpression expression = new CaseExpression(DataTypes.INTEGER, Arrays.asList(
                new Literal(Value.ofBooleanTrue()),
                new Literal(new Value(7, DataTypes.INTEGER)),
                unexpectedElse));

        assertEquals(Integer.valueOf(7), expression.compute().intValue());
    }
}
