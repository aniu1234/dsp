package com.qinyadan.system.dsp.schema.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.MYSQL_DRIVER;


final class MysqlConnectionProvider {

    private final String url;
    private final String username;
    private final String password;

    MysqlConnectionProvider(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password == null ? "" : password;
    }

    Connection open() throws SQLException {
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver is not available", e);
        }
        return DriverManager.getConnection(url, username, password);
    }
}
