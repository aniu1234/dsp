package com.qinyadan.system.dsp.schema.common.bean;

import java.sql.ResultSet;
import java.util.List;


public interface MetaDataHandler<T extends ResultSet> {

    default List<Class> getColumnType(T resultSet) throws IllegalAccessException {
        throw new UnsupportedOperationException("Unsupported operation now...");
    }

    default List<String> getColumnName(T resultSet) throws IllegalAccessException {
        throw new UnsupportedOperationException("Unsupported operation now...");
    }
}
