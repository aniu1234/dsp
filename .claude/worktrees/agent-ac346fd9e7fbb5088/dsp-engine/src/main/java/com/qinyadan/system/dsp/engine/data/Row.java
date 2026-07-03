package com.qinyadan.system.dsp.engine.data;

import java.util.List;


public interface Row<T> {

    /**
     * Size of Column
     *
     * @return
     */
    int columnSize();

    /**
     * @param i, index, start from 0
     * @return value of the column in index
     */
    T getColumn(int i);

    /**
     * Get all column of a row
     *
     * @return
     */
    List<T> getAllColumn();
}
