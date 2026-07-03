package com.qinyadan.system.dsp.schema.common.operator;

import java.util.List;


public class FilterOperator extends AbstractOperator {

    public FilterOperator(Operator next) {
        super(next);
    }

    @Override
    public List<Value> getValue() {
        return super.getValue();
    }

    @Override
    public Operator next() {
        return next;
    }

    @Override
    public void init() {

        super.init();
    }

    @Override
    public void close() {
        super.close();
    }
}
