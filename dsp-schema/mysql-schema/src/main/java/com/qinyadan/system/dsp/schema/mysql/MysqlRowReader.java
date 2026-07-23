package com.qinyadan.system.dsp.schema.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


final class MysqlRowReader {

    private final String sql;
    private final MysqlConnectionProvider connectionProvider;

    MysqlRowReader(String sql, MysqlConnectionProvider connectionProvider) {
        this.sql = sql;
        this.connectionProvider = connectionProvider;
    }

    Iterator<Object[]> readData() {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            while (resultSet.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = resultSet.getObject(i);
                }
                rows.add(row);
            }
            return rows.iterator();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to execute MySQL schema query", e);
        }
    }
}
