package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;


public final class ArithmeticFunction extends Scalar {

    public static final ArithmeticFunction PLUS =
            new ArithmeticFunction(Functions::plus, Functions::plus);
    public static final ArithmeticFunction MINUS =
            new ArithmeticFunction(Functions::minus, Functions::minus);
    public static final ArithmeticFunction MULTIPLY =
            new ArithmeticFunction(Functions::multiply, Functions::multiply);
    public static final ArithmeticFunction DIVIDE =
            new ArithmeticFunction(Functions::divide, Functions::divide);

    private final DoubleBinaryOperator decimalOperation;
    private final LongBinaryOperator integerOperation;

    private ArithmeticFunction(DoubleBinaryOperator decimalOperation,
                               LongBinaryOperator integerOperation) {
        super(2);
        this.decimalOperation = decimalOperation;
        this.integerOperation = integerOperation;
    }

    @Override
    protected Value doEvaluate(List<Value> args, DataType returnType) {
        if (args.stream().anyMatch(Value::isNull)) {
            return Value.nullValue(returnType);
        }

        Value left = args.get(0);
        Value right = args.get(1);
        boolean hasDecimal = args.stream()
                .anyMatch(value -> DataTypes.DECIMAL_TYPES.contains(value.getType()));
        if (hasDecimal) {
            return new Value(decimalOperation.applyAsDouble(
                    left.doubleValue(), right.doubleValue()), returnType);
        }
        return new Value(integerOperation.applyAsLong(
                left.longValue(), right.longValue()), returnType);
    }
}
