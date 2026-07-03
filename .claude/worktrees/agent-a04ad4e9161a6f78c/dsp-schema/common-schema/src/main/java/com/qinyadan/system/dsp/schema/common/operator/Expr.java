package com.qinyadan.system.dsp.schema.common.operator;

import java.util.List;


public interface Expr {

    /**
     * @return
     */
    int getInputParameterCount();

    /**
     * @return
     */
    List<Value> getParameter();

    /**
     * @return
     */
    Value getResult();


    /**
     * compute result
     *
     * @return
     */
    Value compute();

}
