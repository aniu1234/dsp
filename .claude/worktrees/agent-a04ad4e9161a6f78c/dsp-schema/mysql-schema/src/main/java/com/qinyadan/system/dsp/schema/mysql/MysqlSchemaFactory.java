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
        return new MysqlSchema(url, username, password, schema);
    }
}
