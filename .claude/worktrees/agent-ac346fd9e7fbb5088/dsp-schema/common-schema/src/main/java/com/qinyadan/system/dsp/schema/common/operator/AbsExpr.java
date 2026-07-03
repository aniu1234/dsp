package com.qinyadan.system.dsp.schema.common.operator;

import com.google.common.collect.Lists;

import java.util.List;


public class AbsExpr implements Expr {

    private Value input;

    public AbsExpr() {
    }

    @Override
    public int getInputParameterCount() {
        return 1;
    }

    @Override
    public List<Value> getParameter() {
        return Lists.newArrayList(input);
    }

    @Override
    public Value getResult() {
        return null;
    }

    @Override
    public Value compute() {

        //abs
        //利用反射求值
        return null;
    }
}
