package com.qinyadan.system.dsp.schema.common.util;

import com.google.common.collect.Lists;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;


public class ResultSetUtils {

    public static List<Class> getColumnTypeFromResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<Class> types = Lists.newArrayListWithCapacity(metadata.getColumnCount());
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String className = metadata.getColumnClassName(index);
            try {
                types.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                types.add(String.class);
            }
        }
        return types;
    }

    public static List<String> getColumnNameFromResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<String> names = Lists.newArrayListWithCapacity(metadata.getColumnCount());
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            names.add(metadata.getColumnLabel(index));
        }
        return names;
    }

    public static String javaTypeToString(ResultSet rs, int index, Class<?> clzz) throws SQLException {

        final String result = rs.getString(index);
        if (null == result) {
            return null;
        }

        if (clzz == Byte.class) {
            return String.valueOf(rs.getByte(index));
        } else if (clzz == Short.class) {
            return String.valueOf(rs.getShort(index));
        } else if (clzz == Integer.class) {
            return String.valueOf(rs.getInt(index));
        } else if (clzz == Long.class) {
            return String.valueOf(rs.getLong(index));
        } else if (clzz == String.class) {
            return result;
        } else if (clzz == Float.class) {
            return String.valueOf(rs.getFloat(index));
        } else if (clzz == Double.class) {
            return String.valueOf(rs.getDouble(index));
        }

        return result;
    }

    public static Object javaTypeToObject(ResultSet rs, int index, Class<?> clzz) throws SQLException {

        final String result = rs.getString(index);
        if (null == result) {
            return null;
        }

        if (clzz == Byte.class) {
            return rs.getByte(index);
        } else if (clzz == Short.class) {
            return rs.getShort(index);
        } else if (clzz == Integer.class) {
            return rs.getInt(index);
        } else if (clzz == Long.class) {
            return rs.getLong(index);
        } else if (clzz == String.class) {
            return result;
        } else if (clzz == Float.class) {
            return rs.getFloat(index);
        } else if (clzz == Double.class) {
            return rs.getDouble(index);
        }

        return result;
    }
}
