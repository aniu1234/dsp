package com.qinyadan.system.dsp.runtime.util;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Map;
import java.util.Objects;

import static com.qinyadan.system.dsp.runtime.engine.data.type.DataTypes.*;


public class TypeConversionUtils {

    public static final Map<SqlTypeName, DataType> SQL_TYPE_TO_DATA_TYPE = Maps.newHashMap();


    static {
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.INTEGER, INTEGER);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.SMALLINT, SHORT);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.TINYINT, BYTE);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.BIGINT, LONG);

        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.FLOAT, FLOAT);

        //Treat Decimal as double
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.DECIMAL, DOUBLE);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.DOUBLE, DOUBLE);

        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.BOOLEAN, BOOLEAN);

        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.VARCHAR, STRING);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.CHAR, STRING);

        //存储层用long, 但是在表层、展示示需要用实际类型
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.DATE, DATE);
        SQL_TYPE_TO_DATA_TYPE.put(SqlTypeName.TIMESTAMP, LONG);
    }

    public static DataType getBySqlTypeName(SqlTypeName sqlTypeName) {
        DataType r = SQL_TYPE_TO_DATA_TYPE.get(sqlTypeName);
        if (Objects.isNull(r)) {
            throw new UnsupportedOperationException("Currently we do not support type: " + sqlTypeName);
        }

        return r;
    }
}
