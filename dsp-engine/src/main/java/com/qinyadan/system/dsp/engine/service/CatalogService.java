package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.service.dto.CreateColumnDefinition;
import com.qinyadan.system.dsp.engine.service.dto.CreateTableDefinition;
import com.qinyadan.system.dsp.engine.service.dto.ColumnDescription;
import com.qinyadan.system.dsp.engine.service.dto.TableDescription;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stable application-facing entry point for catalog operations.
 *
 * <p>Protocol adapters should depend on this service instead of coordinating
 * Calcite schemas, metadata persistence and table-engine lifecycle directly.</p>
 */
public final class CatalogService implements LifeCycle {

    public static final CatalogService INSTANCE = new CatalogService();

    private final SlothSchemaHolder schemas;

    private CatalogService() {
        this.schemas = SlothSchemaHolder.INSTANCE;
    }

    @Override
    public void init() {
        schemas.init();
    }

    @Override
    public void close() {
        schemas.close();
    }

    public List<String> listDatabases() {
        return Collections.unmodifiableList(schemas.getAllSchemas());
    }

    public boolean databaseExists(String database) {
        return schemas.contains(database);
    }

    SlothSchema getSchema(String database) {
        return schemas.getSlothSchema(database);
    }

    SlothTable getTable(String database, String table) {
        SlothSchema schema = getSchema(database);
        return schema == null ? null : (SlothTable) schema.getTable(table);
    }

    public List<String> listTables(String database) {
        SlothSchema schema = getSchema(database);
        return schema == null ? Collections.<String>emptyList() : schema.getTables();
    }

    public boolean tableExists(String database, String table) {
        SlothSchema schema = getSchema(database);
        return schema != null && schema.containsTable(table);
    }

    public TableDescription describeTable(String database, String tableName) {
        SlothTable table = getTable(database, tableName);
        if (table == null) {
            return null;
        }
        List<ColumnDescription> columns = new ArrayList<>(table.getColumns().size());
        for (SlothColumn column : table.getColumns()) {
            EnhanceSlothColumn definition = column.getColumnType();
            columns.add(new ColumnDescription(column.getColumnName(),
                    definition.getColumnType().getName(), definition.isUnsigned(),
                    definition.isNullable(), definition.getDefalutValue(),
                    definition.getColumnComment(), definition.getPrecision()));
        }
        return new TableDescription(database, tableName, columns,
                table.getEngineName(), table.getShardNum(), table.getTableComment());
    }

    public void createDatabase(String database) {
        schemas.registerSchema(database);
    }

    public boolean dropDatabase(String database) {
        return schemas.removeSchema(database);
    }

    public TableDescription createTable(CreateTableDefinition definition) {
        SlothSchema schema = getSchema(definition.getDatabase());
        if (schema == null) {
            throw new IllegalArgumentException(
                    "Unknown database: " + definition.getDatabase());
        }
        List<SlothColumn> columns = new ArrayList<>(definition.getColumns().size());
        for (CreateColumnDefinition column : definition.getColumns()) {
            EnhanceSlothColumn engineColumn = new EnhanceSlothColumn();
            engineColumn.setColumName(column.getName());
            engineColumn.setColumnType(SqlTypeName.get(column.getSqlType()));
            engineColumn.setUnsigned(column.isUnsigned());
            engineColumn.setNullable(column.isNullable());
            engineColumn.setDefalutValue(column.getDefaultValue());
            engineColumn.setColumnComment(column.getComment());
            engineColumn.setPrecision(column.getPrecision());
            columns.add(new SlothColumn(column.getName(), engineColumn));
        }
        SlothTable table = new SlothTable(schema);
        table.setTableName(definition.getTable());
        table.setColumns(columns);
        table.setShardNum(definition.getShards());
        table.setEngineName(definition.getEngine());
        table.setTableComment(definition.getComment());
        schema.addTable(definition.getTable(), table);
        return describeTable(definition.getDatabase(), definition.getTable());
    }

    public boolean dropTable(String database, String table) {
        SlothSchema schema = getSchema(database);
        return schema != null && schema.dropTable(table);
    }
}
