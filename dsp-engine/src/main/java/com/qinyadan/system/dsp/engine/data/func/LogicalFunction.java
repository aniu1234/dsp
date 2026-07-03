package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;


public abstract class LogicalFunction extends Scalar {

    public LogicalFunction() {
        super(2);
    }

    abstract boolean value(Value v1, Value v2);

    public static final LogicalFunction LOGICAL_AND = new LogicalFunction() {
        @Override
        boolean value(Value v1, Value v2) {
            if (v1.isNull() || v2.isNull()) {
                return false;
            }

            return v1.booleanValue() && v2.booleanValue();
        }
    };

    public static final LogicalFunction LOGICAL_OR = new LogicalFunction() {
        @Override
        boolean value(Value v1, Value v2) {

            //null or null false, null or (not null) true
            if (v1.isNull() && v2.isNull()) {
                return false;
            }

            if (v1.isNull()) {
                return !v2.isNull();
            }

            if (v2.isNull()) {
                return !v2.isNull();
            }

            return v1.booleanValue() || v2.booleanValue();
        }
    };


    @Override
    public Value evaluate(List<Value> args, DataType returnType) {
        final Value v1 = args.get(0);
        final Value v2 = args.get(1);

        return Value.ofBooean(value(v1, v2));
    }
}
