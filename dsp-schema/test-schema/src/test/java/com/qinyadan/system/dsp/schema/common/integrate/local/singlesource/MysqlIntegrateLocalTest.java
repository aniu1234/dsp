package com.qinyadan.system.dsp.schema.common.integrate.local.singlesource;

import com.qinyadan.system.dsp.schema.common.integrate.local.IntegrateLocalTestBase;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


public class MysqlIntegrateLocalTest extends IntegrateLocalTestBase {
    public MysqlIntegrateLocalTest(String inputFile, String resultFile) {
        super(inputFile, resultFile);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sql_and_result/local/file/select/select1.sql", "sql_and_result/local/file/select/select1.txt"},
                {"sql_and_result/local/file/groupby/groupby1.sql", "sql_and_result/local/file/groupby/groupby1.txt"}
        });
    }
}
