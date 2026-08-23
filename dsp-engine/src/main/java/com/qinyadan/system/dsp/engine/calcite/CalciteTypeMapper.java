package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.core.data.type.DataType;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static com.qinyadan.system.dsp.core.data.type.DataTypes.*;

/**
 * Adapts Calcite's SQL type system to DSP's engine-neutral data types.
 *
 * <p>This adapter deliberately lives in {@code dsp-engine}: {@code dsp-core}
 * defines the internal data model and must not depend on a particular SQL
 * planner.</p>
 */
public final class CalciteTypeMapper {

    private static final Map<SqlTypeName, DataType> SQL_TYPES;

    static {
        Map<SqlTypeName, DataType> types = new EnumMap<>(SqlTypeName.class);
        types.put(SqlTypeName.INTEGER, INTEGER);
        types.put(SqlTypeName.SMALLINT, SHORT);
        types.put(SqlTypeName.TINYINT, BYTE);
        types.put(SqlTypeName.BIGINT, LONG);
        types.put(SqlTypeName.FLOAT, FLOAT);
        types.put(SqlTypeName.DECIMAL, DOUBLE);
        types.put(SqlTypeName.DOUBLE, DOUBLE);
        types.put(SqlTypeName.BOOLEAN, BOOLEAN);
        types.put(SqlTypeName.VARCHAR, STRING);
        types.put(SqlTypeName.CHAR, STRING);
        types.put(SqlTypeName.DATE, DATE);
        types.put(SqlTypeName.TIMESTAMP, TIMESTAMP);
        SQL_TYPES = Collections.unmodifiableMap(types);
    }

    private CalciteTypeMapper() {
    }

    public static DataType toDataType(SqlTypeName sqlTypeName) {
        DataType type = SQL_TYPES.get(sqlTypeName);
        if (type == null) {
            throw new UnsupportedOperationException(
                    "Currently we do not support type: " + sqlTypeName);
        }
        return type;
    }
}
