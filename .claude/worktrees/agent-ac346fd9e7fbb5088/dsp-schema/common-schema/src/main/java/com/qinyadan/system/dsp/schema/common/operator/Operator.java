package com.qinyadan.system.dsp.schema.common.operator;

import java.util.List;


public interface Operator {


    List<Value> getValue();

    /**
     * do init() work
     */
    void init();

    /**
     * main work
     *
     * @return
     */
    Operator next();

    /**
     * do close work
     */
    void close();
}


