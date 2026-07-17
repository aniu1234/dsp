package com.qinyadan.system.dsp.schema.common.integrate.remote;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import org.junit.Assume;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.MYSQL_DRIVER;
import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.OBJECT_MAPPER;


@Slf4j
public abstract class IntegrateRemoteSqlTest extends IntegrateRemoteTestBase {

    private final String connectionConf;
    private Connection connection;

    public IntegrateRemoteSqlTest(String filePath, String connectionConf) {
        super(filePath);
        this.connectionConf = connectionConf;
    }

    @Override
    public void after() {
        super.after();
        try {
            if (Objects.nonNull(connection) && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error(e.toString());
        }
    }

    /**
     * Default implementation is mysql
     *
     * @return
     */
    @Override
    public Statement getStatement() {
        String enabled = System.getProperty("dsp.test.mysql.enabled",
                System.getenv("DSP_TEST_MYSQL_ENABLED"));
        Assume.assumeTrue("External MySQL integration test is disabled",
                Boolean.parseBoolean(enabled));
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(connectionConf)) {
            final JsonNode node = OBJECT_MAPPER.readTree(inputStream);

            final String url = config("dsp.test.mysql.url", "DSP_TEST_MYSQL_URL",
                    node.get("url").textValue());
            final String username = config("dsp.test.mysql.username", "DSP_TEST_MYSQL_USERNAME",
                    node.get("username").textValue());
            final String password = config("dsp.test.mysql.password", "DSP_TEST_MYSQL_PASSWORD",
                    node.get("password").textValue());
            final String defaultSchema = config("dsp.test.mysql.schema", "DSP_TEST_MYSQL_SCHEMA",
                    node.get("schema").textValue());

            Class.forName(MYSQL_DRIVER);
            connection = DriverManager.getConnection(url, username, password);
            connection.setSchema(defaultSchema);
            return connection.createStatement();
        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    private static String config(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(environment);
        }
        return value == null ? fallback : value;
    }

}
