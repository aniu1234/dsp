package com.qinyadan.system.dsp.runtime.engine.data.func;

import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;

import java.util.List;


public abstract class Scalar {

    protected int arglength;

    public Scalar(int arglength) {
        this.arglength = arglength;
    }

    public abstract Value evaluate(List<Value> args, DataType returnType);
}
