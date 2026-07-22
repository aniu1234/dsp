package com.qinyadan.system.dsp.schema.common.integrate.local.multisource;

import com.qinyadan.system.dsp.schema.common.integrate.local.IntegrateLocalTestBase;
import org.junit.runners.Parameterized;
import org.junit.Before;
import org.junit.Assume;

import java.util.Arrays;
import java.util.Collection;


public class FileAndMysqlJoinIntegrateLocalTest extends IntegrateLocalTestBase {

    public FileAndMysqlJoinIntegrateLocalTest(String inputFile, String resultFile) {
        super(inputFile, resultFile);
    }

    @Before
    public void requireExternalMysql() {
        String enabled = System.getProperty("dsp.test.mysql.enabled",
                System.getenv("DSP_TEST_MYSQL_ENABLED"));
        Assume.assumeTrue("External MySQL integration test is disabled",
                Boolean.parseBoolean(enabled));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sql_and_result/local/file/join/join1.sql", "sql_and_result/local/file/join/join1.txt"},
        });
    }
}
