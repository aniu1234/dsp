package com.qinyadan.system.dsp.schema.common.integrate.local.multisource;

import com.qinyadan.system.dsp.schema.common.integrate.local.IntegrateLocalTestBase;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;


public class FileAndMysqlJoinIntegrateLocalTest extends IntegrateLocalTestBase {

    public FileAndMysqlJoinIntegrateLocalTest(String inputFile, String resultFile) {
        super(inputFile, resultFile);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sql_and_result/local/file/join/join1.sql", "sql_and_result/local/file/join/join1.txt"},
        });
    }
}
