package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public abstract class LogicalFunction extends Scalar {

    public LogicalFunction() {
        super(2);
    }

    abstract Boolean value(Value v1, Value v2);

    public static final LogicalFunction LOGICAL_AND = new LogicalFunction() {
        @Override
        Boolean value(Value v1, Value v2) {
            if ((!v1.isNull() && !v1.booleanValue())
                    || (!v2.isNull() && !v2.booleanValue())) {
                return false;
            }
            if (v1.isNull() || v2.isNull()) {
                return null;
            }
            return true;
        }
    };

    public static final LogicalFunction LOGICAL_OR = new LogicalFunction() {
        @Override
        Boolean value(Value v1, Value v2) {
            if ((!v1.isNull() && v1.booleanValue())
                    || (!v2.isNull() && v2.booleanValue())) {
                return true;
            }
            if (v1.isNull() || v2.isNull()) {
                return null;
            }
            return false;
        }
    };


    @Override
    protected Value doEvaluate(List<Value> args, DataType returnType) {
        final Value v1 = args.get(0);
        final Value v2 = args.get(1);

        return new Value(value(v1, v2), DataTypes.BOOLEAN);
    }
}
