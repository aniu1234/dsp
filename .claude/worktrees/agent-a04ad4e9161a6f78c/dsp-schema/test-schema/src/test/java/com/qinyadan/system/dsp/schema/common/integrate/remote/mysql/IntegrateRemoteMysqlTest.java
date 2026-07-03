package com.qinyadan.system.dsp.schema.common.integrate.remote.mysql;

import com.qinyadan.system.dsp.schema.common.integrate.remote.IntegrateRemoteSqlTest;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


public class IntegrateRemoteMysqlTest extends IntegrateRemoteSqlTest {
    public IntegrateRemoteMysqlTest(String filePath, String connectionConf) {
        super(filePath, connectionConf);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sql_and_result/remote/mysql/select/select1.sql", "sql_and_result/remote/mysql/mysql_config.json"}
        });
    }
}
