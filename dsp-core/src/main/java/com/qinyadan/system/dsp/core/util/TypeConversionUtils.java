package com.qinyadan.system.dsp.core.util;

import com.qinyadan.system.dsp.core.data.type.DataType;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static com.qinyadan.system.dsp.core.data.type.DataTypes.*;


public final class TypeConversionUtils {

    private static final Map<SqlTypeName, DataType> SQL_TYPE_TO_DATA_TYPE;


    static {
        Map<SqlTypeName, DataType> types = new EnumMap<>(SqlTypeName.class);
        types.put(SqlTypeName.INTEGER, INTEGER);
        types.put(SqlTypeName.SMALLINT, SHORT);
        types.put(SqlTypeName.TINYINT, BYTE);
        types.put(SqlTypeName.BIGINT, LONG);

        types.put(SqlTypeName.FLOAT, FLOAT);

        //Treat Decimal as double
        types.put(SqlTypeName.DECIMAL, DOUBLE);
        types.put(SqlTypeName.DOUBLE, DOUBLE);

        types.put(SqlTypeName.BOOLEAN, BOOLEAN);

        types.put(SqlTypeName.VARCHAR, STRING);
        types.put(SqlTypeName.CHAR, STRING);

        //存储层用long, 但是在表层、展示示需要用实际类型
        types.put(SqlTypeName.DATE, DATE);
        types.put(SqlTypeName.TIMESTAMP, TIMESTAMP);
        SQL_TYPE_TO_DATA_TYPE = Collections.unmodifiableMap(types);
    }

    private TypeConversionUtils() {
    }

    public static DataType getBySqlTypeName(SqlTypeName sqlTypeName) {
        DataType r = SQL_TYPE_TO_DATA_TYPE.get(sqlTypeName);
        if (Objects.isNull(r)) {
            throw new UnsupportedOperationException("Currently we do not support type: " + sqlTypeName);
        }

        return r;
    }
}
