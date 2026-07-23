package com.qinyadan.system.dsp.schema.mysql;

import org.junit.Test;


public class MysqlSchemaTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankSchemaNameBeforeOpeningConnection() {
        new MysqlSchema("jdbc:mysql://localhost/test", "user", "password", "  ");
    }
}
