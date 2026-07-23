package com.qinyadan.system.dsp.engine.operator;

import com.qinyadan.system.dsp.core.data.type.DataType;

import java.util.List;


public interface Operator<R> {

    /**
     * Open operator, do some init work
     */
    void open();

    /**
     * Get next row, next has another value to identify the value type
     * <p>
     * R is the row type
     *
     * @return
     */
    R next();

    /**
     * do close work, eg, you can close resource/network
     */
    void close();

    /**
     * Get return type of this operator
     *
     * @return
     */
    List<DataType> getRowType();
}
