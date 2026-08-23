package com.qinyadan.system.dsp.schema.mysql;

import com.qinyadan.system.dsp.schema.common.spi.ConnectorHealth;
import com.qinyadan.system.dsp.schema.common.spi.SchemaConnector;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;


public class MysqlSchemaFactory implements SchemaConnector {

    @Override
    public String id() {
        return "mysql";
    }

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        MysqlOptions options = MysqlOptions.from(operand);
        return new MysqlSchema(options.connectionProvider(), options.schema());
    }

    @Override
    public ConnectorHealth health(Map<String, Object> operand) {
        try {
            MysqlOptions options = MysqlOptions.from(operand);
            try (Connection connection = options.connectionProvider().open()) {
                if (!connection.isValid(2)) {
                    return ConnectorHealth.unavailable("MySQL connection validation failed");
                }
            }
            return ConnectorHealth.ready("MySQL connector is reachable");
        } catch (RuntimeException | SQLException e) {
            return ConnectorHealth.unavailable("MySQL connector is unavailable", e);
        }
    }
}
