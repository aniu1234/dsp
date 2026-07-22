package com.qinyadan.system.dsp.schema.mysql;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

import java.util.Map;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.*;


public class MysqlSchemaFactory implements SchemaFactory {

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        String url = (String) operand.get(URL);
        String username = (String) operand.get(USER_NAME);
        String password = (String) operand.get(PASSWORD);
        String schema = (String) operand.get(SCHEMA);
        url = config("dsp.schema.mysql.url", "DSP_SCHEMA_MYSQL_URL", url);
        username = config("dsp.schema.mysql.username", "DSP_SCHEMA_MYSQL_USERNAME", username);
        password = config("dsp.schema.mysql.password", "DSP_SCHEMA_MYSQL_PASSWORD", password);
        schema = config("dsp.schema.mysql.schema", "DSP_SCHEMA_MYSQL_SCHEMA", schema);
        return new MysqlSchema(url, username, password, schema);
    }

    private static String config(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(environment);
        }
        return value == null ? fallback : value;
    }
}
