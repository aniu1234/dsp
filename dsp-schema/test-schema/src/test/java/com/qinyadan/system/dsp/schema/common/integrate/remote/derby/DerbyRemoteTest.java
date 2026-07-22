package com.qinyadan.system.dsp.schema.common.integrate.remote.derby;

import com.qinyadan.system.dsp.schema.common.integrate.local.IntegrateLocalTestBase;
import com.qinyadan.system.dsp.schema.common.integrate.remote.IntegrateRemoteTestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.runners.Parameterized;
import org.junit.Assume;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.DERBY_DRIVER;
import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.DERBY_URL;


@Slf4j
public class DerbyRemoteTest extends IntegrateRemoteTestBase {
    private final String metaFile;
    private Connection connection;

    public DerbyRemoteTest(String filePath, String metaFile) {
        super(filePath);
        this.metaFile = metaFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sql_and_result/remote/derby/select/select1.sql", "sql_and_result/remote/derby/derby.sql"}
        });
    }

    @Override
    public void after() {
        super.after();
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("close derby connection:" + e.getMessage());
            }
        }
    }

    @Override
    public void init() {
        String enabled = System.getProperty("dsp.test.mysql.enabled",
                System.getenv("DSP_TEST_MYSQL_ENABLED"));
        Assume.assumeTrue("Derby comparison suite also queries the external MySQL schema",
                Boolean.parseBoolean(enabled));
        super.init();
        try {

            //create schema and table in memory
            final InputStream inputSqlStream = IntegrateLocalTestBase.class.getClassLoader().getResourceAsStream(metaFile);
            java.util.List<String> sqls = loadSqlStatements(inputSqlStream);

            for (String sql : sqls) {
                dbStatement.execute(sql);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public Statement getStatement() {

        try {
            System.setProperty("derby.stream.error.file", "target/derby.log");
            Class.forName(DERBY_DRIVER).newInstance();
            connection = DriverManager.getConnection(DERBY_URL);
            return connection.createStatement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
