package com.qinyadan.system.dsp.storage.meta;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.util.mysql.MySQLDSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;


@Slf4j
public class MysqlConnection {

    public static final MysqlConnection INSTANCE = new MysqlConnection();

    private boolean isOk = true;
    private String addr;
    private String username;
    private String password;
    private Connection connection = null;
    private DSLContext dslContext;

    public MysqlConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            //TODO make configuration
            addr = "jdbc:mysql://localhost:3306";
            username = "root";
            password = "qinyadan";

            getConnection();

            dslContext = MySQLDSL.using(connection, SQLDialect.MYSQL);
        } catch (ClassNotFoundException e) {
            log.error("Can't load mysql driver for:", e);
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            log.error("This means your jdbc driver is 5.x and mysql server is 8.x, you need update your jdbc connector");
            throw new RuntimeException(e);
        } catch (RuntimeException e1) {
            log.error("init meta database failed, mark meta database is fail, then all db/table you create could not be store");
            isOk = false;
        }
    }


    public Connection getConnection() {
        try {
            if (Objects.nonNull(connection) && !connection.isClosed()) {
                return connection;
            }
            connection = DriverManager.getConnection(addr, username, password);
            return connection;
        } catch (SQLException e) {
            log.info("can't get connection:", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isOk() {
        if (!isOk) {
            log.warn("Meta database is abnormal, attention...");
        }

        return isOk;
    }

    public DSLContext getDslContext() {
        return dslContext;
    }
}
