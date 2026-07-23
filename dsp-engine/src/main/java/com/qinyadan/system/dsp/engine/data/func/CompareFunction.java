package com.qinyadan.system.dsp.engine.data.func;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.List;


public abstract class CompareFunction extends Scalar {

    public CompareFunction() {
        super(2);
    }

    public static final CompareFunction LESS_THAN = new CompareFunction() {
        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) < 0;
        }
    };
    public static final CompareFunction LESS_THAN_OR_EQUAL = new CompareFunction() {
        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) <= 0;
        }

    };
    public static final CompareFunction GREATER_THAN = new CompareFunction() {
        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) > 0;
        }
    };
    public static final CompareFunction GREATER_THAN_OR_EQUAL = new CompareFunction() {
        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) >= 0;
        }
    };
    public static final CompareFunction EQUALS = new CompareFunction() {
        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) == 0;
        }
    };
    public static final CompareFunction NOT_EQUALS = new CompareFunction() {

        @Override
        public boolean compare(Value v1, Value v2) {
            return v1.compareTo(v2) != 0;
        }
    };

    @Override
    protected Value doEvaluate(List<Value> args, DataType returnType) {
        final Value v1 = args.get(0);
        final Value v2 = args.get(1);

        if (v1.isNull() || v2.isNull()) {
            return new Value(null, DataTypes.BOOLEAN);
        }
        return Value.ofBoolean(compare(v1, v2));
    }

    /**
     * @param v1 first value
     * @param v2 second value
     * @return result of first value compared to the second value
     */
    public abstract boolean compare(Value v1, Value v2);

}
