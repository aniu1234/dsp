package com.qinyadan.system.dsp.schema.mysql;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import com.qinyadan.system.dsp.schema.common.config.SchemaConfig;

import java.util.Map;

import static com.qinyadan.system.dsp.schema.common.constants.CommonConstant.*;


public class MysqlSchemaFactory implements SchemaFactory {

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        String url = SchemaConfig.requireResolved(URL, "dsp.schema.mysql.url",
                "DSP_SCHEMA_MYSQL_URL", SchemaConfig.optionalString(operand, URL, null));
        String username = SchemaConfig.requireResolved(USER_NAME, "dsp.schema.mysql.username",
                "DSP_SCHEMA_MYSQL_USERNAME", SchemaConfig.optionalString(operand, USER_NAME, null));
        String password = SchemaConfig.resolve("dsp.schema.mysql.password",
                "DSP_SCHEMA_MYSQL_PASSWORD", SchemaConfig.optionalString(operand, PASSWORD, ""));
        String schema = SchemaConfig.requireResolved(SCHEMA, "dsp.schema.mysql.schema",
                "DSP_SCHEMA_MYSQL_SCHEMA", SchemaConfig.optionalString(operand, SCHEMA, null));
        return new MysqlSchema(url, username, password, schema);
    }
}
