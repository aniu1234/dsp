package com.qinyadan.system.dsp.protocol.utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;


public final class StringUtil {

    private StringUtil() {
    }

    public static String[] getDbAndTableName(String tableWithDb) {
        return tableWithDb.split("\\.", 2);
    }

    public static Pair<String, String> getDbAndTablePair(String tableWithDb, String dbFromConn) {
        final String[] tableAndDatabase = StringUtil.getDbAndTableName(tableWithDb);

        final String table;
        final String db;
        if (tableAndDatabase.length == 1) {
            table = tableAndDatabase[0];
            db = dbFromConn;
        } else {
            db = tableAndDatabase[0];
            table = tableAndDatabase[1];
        }

        return ImmutablePair.of(db, table);
    }
}
