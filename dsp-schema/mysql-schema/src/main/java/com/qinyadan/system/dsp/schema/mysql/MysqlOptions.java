package com.qinyadan.system.dsp.schema.mysql;

import com.qinyadan.system.dsp.schema.common.config.SchemaConfig;

import java.util.Map;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.PASSWORD;
import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.SCHEMA;
import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.URL;
import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.USER_NAME;


final class MysqlOptions {

    private final String url;
    private final String username;
    private final String password;
    private final String schema;

    private MysqlOptions(String url, String username, String password, String schema) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.schema = schema;
    }

    static MysqlOptions from(Map<String, Object> operand) {
        String url = SchemaConfig.requireResolved(URL, "dsp.schema.mysql.url",
                "DSP_SCHEMA_MYSQL_URL", SchemaConfig.optionalString(operand, URL, null));
        String username = SchemaConfig.requireResolved(USER_NAME, "dsp.schema.mysql.username",
                "DSP_SCHEMA_MYSQL_USERNAME", SchemaConfig.optionalString(operand, USER_NAME, null));
        String password = SchemaConfig.resolve("dsp.schema.mysql.password",
                "DSP_SCHEMA_MYSQL_PASSWORD", SchemaConfig.optionalString(operand, PASSWORD, ""));
        String schema = SchemaConfig.requireResolved(SCHEMA, "dsp.schema.mysql.schema",
                "DSP_SCHEMA_MYSQL_SCHEMA", SchemaConfig.optionalString(operand, SCHEMA, null));
        return new MysqlOptions(url, username, password == null ? "" : password, schema);
    }

    MysqlConnectionProvider connectionProvider() {
        return new MysqlConnectionProvider(url, username, password);
    }

    String schema() {
        return schema;
    }
}
