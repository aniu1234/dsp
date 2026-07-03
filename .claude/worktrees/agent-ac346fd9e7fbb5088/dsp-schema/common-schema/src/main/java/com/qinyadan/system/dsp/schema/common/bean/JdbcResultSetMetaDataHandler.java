package com.qinyadan.system.dsp.schema.common.bean;

import com.mysql.cj.jdbc.result.ResultSetImpl;
import com.qinyadan.system.dsp.schema.common.util.ReflectionUtils;
import com.qinyadan.system.dsp.schema.common.util.TypeUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class JdbcResultSetMetaDataHandler implements MetaDataHandler<ResultSetImpl> {

    /**
     * For JDBC result set
     */
    private static final Field JDBC42_RESULT_SET_FIELDS =
            ReflectionUtils.getField(ResultSetImpl.class, "columnDefinition");

    @Override
    public List<Class> getColumnType(ResultSetImpl resultSet) throws IllegalAccessException {
        final com.mysql.cj.result.Field[] columnMetaDataList =
                (com.mysql.cj.result.Field[]) JDBC42_RESULT_SET_FIELDS.get(resultSet);

        return Arrays.stream(columnMetaDataList)
                .map(com.mysql.cj.result.Field::getMysqlType)
                .map(TypeUtil::mysqlTypeToClass)
                .collect(Collectors.toList());
    }


    @Override
    public List<String> getColumnName(ResultSetImpl resultSet) throws IllegalAccessException {
        final com.mysql.cj.result.Field[] columnMetaDataList =
                (com.mysql.cj.result.Field[]) JDBC42_RESULT_SET_FIELDS.get(resultSet);

        return Arrays.stream(columnMetaDataList).map(f -> {
            return f.getName();
        }).collect(Collectors.toList());
    }
}
