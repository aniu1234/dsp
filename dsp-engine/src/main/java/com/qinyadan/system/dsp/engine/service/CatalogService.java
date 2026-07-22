package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumnType;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlCreateTable;
import org.apache.calcite.sql.SqlNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Application boundary for schema and table lifecycle operations.
 */
public final class CatalogService {

    public static final CatalogService INSTANCE = new CatalogService(SlothSchemaHolder.INSTANCE);

    private final SlothSchemaHolder schemas;

    CatalogService(SlothSchemaHolder schemas) {
        this.schemas = Objects.requireNonNull(schemas, "schemas");
    }

    public boolean schemaExists(String database) {
        return database != null && schemas.contains(database);
    }

    public void createSchema(String database) {
        if (schemaExists(database)) {
            throw new CatalogException(Reason.SCHEMA_ALREADY_EXISTS, database);
        }
        try {
            schemas.registerSchema(database);
        } catch (IllegalStateException e) {
            if (schemaExists(database)) {
                throw new CatalogException(Reason.SCHEMA_ALREADY_EXISTS, database, e);
            }
            throw e;
        }
    }

    public boolean dropSchema(String database) {
        return schemas.removeSchema(database);
    }

    public List<String> listSchemas() {
        List<String> result = schemas.getAllSchemas();
        Collections.sort(result);
        return result;
    }

    public boolean tableExists(String database, String table) {
        SlothSchema schema = schemas.getSlothSchema(database);
        return schema != null && schema.containsTable(table);
    }

    public List<String> listTables(String database) {
        SlothSchema schema = requireSchema(database);
        List<String> result = schema.getTables();
        Collections.sort(result);
        return result;
    }

    public boolean dropTable(String database, String table) {
        SlothSchema schema = schemas.getSlothSchema(database);
        return schema != null && schema.containsTable(table) && schema.dropTable(table);
    }

    public void createTable(String database, String tableName, SqlCreateTable definition) {
        SlothSchema schema = requireSchema(database);
        if (schema.containsTable(tableName)) {
            throw new CatalogException(Reason.TABLE_ALREADY_EXISTS, tableName);
        }

        List<SqlNode> nodes = definition.getNameAndType().getList();
        List<SlothColumn> columns = new ArrayList<>(nodes.size() / 2);
        Set<String> columnNames = new HashSet<>();
        for (int i = 0; i < nodes.size() / 2; i++) {
            String columnName = nodes.get(2 * i).toString();
            if (!columnNames.add(columnName)) {
                throw new CatalogException(Reason.DUPLICATE_COLUMN, columnName);
            }
            SlothColumnType columnType = (SlothColumnType) nodes.get(2 * i + 1);
            columns.add(new SlothColumn(columnName, columnType.toEnhance(columnName)));
        }

        SlothTable table = new SlothTable(schema);
        table.setTableName(tableName);
        table.setColumns(columns);
        table.setShardNum(definition.getShard());
        table.setEngineName(definition.getEngine() == null
                ? SlothTable.DEFAULT_ENGINE_NAME : definition.getEngine());
        if (definition.getTableComment() != null) {
            table.setTableComment(definition.getTableComment().toString());
        }
        table.initTableEngine();
        schema.addTable(tableName, table);
    }

    public TableDefinition describeTable(String database, String tableName) {
        SlothSchema schema = requireSchema(database);
        SlothTable table = (SlothTable) schema.getTable(tableName);
        if (table == null) {
            throw new CatalogException(Reason.TABLE_NOT_FOUND, tableName);
        }

        List<ColumnDefinition> columns = new ArrayList<>(table.getColumns().size());
        for (SlothColumn column : table.getColumns()) {
            EnhanceSlothColumn definition = column.getColumnType();
            columns.add(new ColumnDefinition(column.getColumnName(),
                    definition.getColumnType().getName(), definition.isUnsigned(),
                    definition.isNullable(), definition.getDefalutValue(),
                    definition.getColumnComment(), definition.getPrecision()));
        }
        String engineName = table.getEngineName() == null
                ? SlothTable.DEFAULT_ENGINE_NAME : table.getEngineName();
        return new TableDefinition(table.getTableName(), engineName, table.getShardNum(),
                table.getTableComment(), columns);
    }

    private SlothSchema requireSchema(String database) {
        SlothSchema schema = schemas.getSlothSchema(database);
        if (schema == null) {
            throw new CatalogException(Reason.SCHEMA_NOT_FOUND, database);
        }
        return schema;
    }

    public enum Reason {
        SCHEMA_ALREADY_EXISTS,
        SCHEMA_NOT_FOUND,
        TABLE_ALREADY_EXISTS,
        TABLE_NOT_FOUND,
        DUPLICATE_COLUMN
    }

    public static final class CatalogException extends RuntimeException {

        private final Reason reason;
        private final String objectName;

        CatalogException(Reason reason, String objectName) {
            this(reason, objectName, null);
        }

        CatalogException(Reason reason, String objectName, Throwable cause) {
            super(reason + ": " + objectName, cause);
            this.reason = reason;
            this.objectName = objectName;
        }

        public Reason getReason() {
            return reason;
        }

        public String getObjectName() {
            return objectName;
        }
    }

    public static final class TableDefinition {

        private final String name;
        private final String engineName;
        private final int shardCount;
        private final String comment;
        private final List<ColumnDefinition> columns;

        TableDefinition(String name, String engineName, int shardCount, String comment,
                        List<ColumnDefinition> columns) {
            this.name = name;
            this.engineName = engineName;
            this.shardCount = shardCount;
            this.comment = comment;
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        }

        public String getName() { return name; }
        public String getEngineName() { return engineName; }
        public int getShardCount() { return shardCount; }
        public String getComment() { return comment; }
        public List<ColumnDefinition> getColumns() { return columns; }
    }

    public static final class ColumnDefinition {

        private final String name;
        private final String typeName;
        private final boolean unsigned;
        private final boolean nullable;
        private final String defaultValue;
        private final String comment;
        private final int precision;

        ColumnDefinition(String name, String typeName, boolean unsigned, boolean nullable,
                         String defaultValue, String comment, int precision) {
            this.name = name;
            this.typeName = typeName;
            this.unsigned = unsigned;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.comment = comment;
            this.precision = precision;
        }

        public String getName() { return name; }
        public String getTypeName() { return typeName; }
        public boolean isUnsigned() { return unsigned; }
        public boolean isNullable() { return nullable; }
        public String getDefaultValue() { return defaultValue; }
        public String getComment() { return comment; }
        public int getPrecision() { return precision; }
    }
}
