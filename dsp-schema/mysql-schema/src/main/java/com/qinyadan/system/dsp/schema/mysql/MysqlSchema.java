package com.qinyadan.system.dsp.schema.mysql;

import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


public class MysqlSchema extends AbstractSchema {

    private static final String TABLES_SQL = "select table_name from information_schema.tables "
            + "where table_type = ? and table_schema = ?";
    private static final String BASE_TABLE = "BASE TABLE";

    private final MysqlConnectionProvider connectionProvider;
    private final String schema;
    private volatile Map<String, Table> tableMap;

    public MysqlSchema(String url, String username, String password, String schema) {
        this(new MysqlConnectionProvider(url, username, password), schema);
    }

    MysqlSchema(MysqlConnectionProvider connectionProvider, String schema) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("MySQL connection provider must not be null");
        }
        if (schema == null || schema.trim().isEmpty()) {
            throw new IllegalArgumentException("MySQL schema must not be empty");
        }
        this.connectionProvider = connectionProvider;
        this.schema = schema.trim();
    }

    @Override
    protected Map<String, Table> getTableMap() {
        Map<String, Table> tables = tableMap;
        if (tables != null) {
            return tables;
        }
        synchronized (this) {
            if (tableMap == null) {
                tableMap = loadTables();
            }
            return tableMap;
        }
    }

    private Map<String, Table> loadTables() {
        Map<String, Table> sortedTables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(TABLES_SQL)) {
            statement.setString(1, BASE_TABLE);
            statement.setString(2, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString(1);
                    sortedTables.put(tableName,
                            new MysqlTable(schema, tableName, connectionProvider));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load MySQL schema '" + schema + "'", e);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sortedTables));
    }
}
