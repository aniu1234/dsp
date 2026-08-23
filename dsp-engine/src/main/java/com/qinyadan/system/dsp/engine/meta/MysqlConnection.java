package com.qinyadan.system.dsp.engine.meta;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
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

    private boolean isOk;
    private String addr;
    private String username;
    private String password;
    private Connection connection = null;
    private DSLContext dslContext;

    public MysqlConnection() {
        DspConfiguration configuration = DspConfiguration.load();
        addr = configuration.getMetadataJdbcUrl();
        username = configuration.getMetadataJdbcUsername();
        password = configuration.getMetadataJdbcPassword();

        if (addr == null || addr.trim().isEmpty()) {
            log.info("Metadata database is disabled; set DSP_META_JDBC_URL to enable it");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            getConnection();
            dslContext = MySQLDSL.using(connection, SQLDialect.MYSQL);
            isOk = true;
        } catch (ClassNotFoundException e) {
            log.error("Can't load mysql driver for:", e);
            isOk = false;
        } catch (NullPointerException e) {
            log.error("This means your jdbc driver is 5.x and mysql server is 8.x, you need update your jdbc connector");
            isOk = false;
        } catch (RuntimeException e1) {
            log.error("Failed to initialize metadata database; metadata persistence is disabled", e1);
            isOk = false;
        }
    }

    public Connection getConnection() {
        try {
            if (Objects.nonNull(connection) && !connection.isClosed()) {
                return connection;
            }
            connection = DriverManager.getConnection(addr,
                    username == null ? "" : username,
                    password == null ? "" : password);
            return connection;
        } catch (SQLException e) {
            log.info("can't get connection:", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isOk() {
        return isOk;
    }

    public DSLContext getDslContext() {
        return dslContext;
    }
}
