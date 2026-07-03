package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;

public interface Aggregation {

    /**
     * compute result
     *
     * @return
     */
    Value compute();


    /**
     * @return
     */
    DataType getResultType();


    /**
     * @return
     */
    DataType inputType();


    /**
     * @return
     */
    boolean isDistinct();


    /**
     * @return
     */
    boolean ignoreNulls();


    /**
     * add a Value
     *
     * @param v
     */
    default void add(Value v) {
        //todo
    }

    /**
     *
     */
    void init();

}
