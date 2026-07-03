package com.qinyadan.system.dsp.schema.mysql;

import java.util.Iterator;


public interface MysqlReader {

    /**
     * MySQL reader to read data
     *
     * @return
     */
    Iterator<Object[]> readData();
}
