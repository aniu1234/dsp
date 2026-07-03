package com.qinyadan.system.dsp.schema.common.operator;

import java.util.List;


public abstract class AbstractOperator implements Operator {

    protected Operator next;

    public AbstractOperator(Operator next) {
        this.next = next;
    }

    @Override
    public List<Value> getValue() {
        return next().getValue();
    }

    @Override
    public Operator next() {
        throw new UnsupportedOperationException("current do not support");
    }

    @Override
    public void init() {

    }

    @Override
    public void close() {

    }
}
