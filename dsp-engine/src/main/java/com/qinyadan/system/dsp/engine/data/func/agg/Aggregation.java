package com.qinyadan.system.dsp.engine.data.func.agg;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;

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


}
