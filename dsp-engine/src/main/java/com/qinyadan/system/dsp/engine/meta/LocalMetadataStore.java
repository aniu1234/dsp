package com.qinyadan.system.dsp.engine.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.constant.FileConstants;
import org.apache.calcite.sql.type.SqlTypeName;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Atomic, built-in catalog used when an external metadata database is not configured.
 */
public class LocalMetadataStore {

    static final int CATALOG_VERSION = 1;
    static final String CATALOG_FILE = "catalog-v1.json";

    public static final LocalMetadataStore INSTANCE = new LocalMetadataStore(
            Paths.get(FileConstants.getTableFileLocation(), "_meta", CATALOG_FILE));

    private final Path catalogPath;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CatalogState state;

    LocalMetadataStore(Path catalogPath) {
        this.catalogPath = catalogPath;
    }

    public synchronized Set<String> allSchemas() {
        load();
        return new LinkedHashSet<>(state.schemas.keySet());
    }

    public synchronized void addSchema(String schema) {
        load();
        if (!state.schemas.containsKey(schema)) {
            state.schemas.put(schema, new SchemaState());
            try {
                persist();
            } catch (RuntimeException e) {
                state.schemas.remove(schema);
                throw e;
            }
        }
    }

    public synchronized void dropSchema(String schema) {
        load();
        SchemaState removed = state.schemas.remove(schema);
        if (removed != null) {
            try {
                persist();
            } catch (RuntimeException e) {
                state.schemas.put(schema, removed);
                throw e;
            }
        }
    }

    public synchronized void addTable(String schemaName, SlothTable table) {
        load();
        SchemaState schema = state.schemas.get(schemaName);
        if (schema == null) {
            throw new IllegalStateException("Schema does not exist in catalog: " + schemaName);
        }
        if (schema.tables.containsKey(table.getTableName())) {
            throw new IllegalStateException("Table already exists in catalog: "
                    + schemaName + "." + table.getTableName());
        }
        schema.tables.put(table.getTableName(), TableState.from(table));
        try {
            persist();
        } catch (RuntimeException e) {
            schema.tables.remove(table.getTableName());
            throw e;
        }
    }

    public synchronized void deleteTable(String schemaName, String tableName) {
        load();
        SchemaState schema = state.schemas.get(schemaName);
        if (schema == null) {
            return;
        }
        TableState removed = schema.tables.remove(tableName);
        if (removed == null) {
            return;
        }
        try {
            persist();
        } catch (RuntimeException e) {
            schema.tables.put(tableName, removed);
            throw e;
        }
    }

    public synchronized List<SlothTable> getAllTables(SlothSchema schema) {
        load();
        SchemaState schemaState = state.schemas.get(schema.getSchemaName());
        List<SlothTable> result = new ArrayList<>();
        if (schemaState == null) {
            return result;
        }
        for (Map.Entry<String, TableState> entry : schemaState.tables.entrySet()) {
            result.add(entry.getValue().toTable(entry.getKey(), schema));
        }
        return result;
    }

    private void load() {
        if (state != null) {
            return;
        }
        if (!Files.exists(catalogPath)) {
            state = new CatalogState();
            return;
        }
        try {
            state = objectMapper.readValue(catalogPath.toFile(), CatalogState.class);
            if (state.version != CATALOG_VERSION) {
                throw new IllegalStateException("Unsupported catalog version " + state.version
                        + " at " + catalogPath);
            }
            if (state.schemas == null) {
                state.schemas = new LinkedHashMap<>();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load metadata catalog " + catalogPath, e);
        }
    }

    private void persist() {
        Path parent = catalogPath.getParent();
        Path temporary = catalogPath.resolveSibling(catalogPath.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), state);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temporary, catalogPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, catalogPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot persist metadata catalog " + catalogPath, e);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The next successful catalog write will replace this temporary file.
            }
        }
    }

    public static class CatalogState {
        public int version = CATALOG_VERSION;
        public Map<String, SchemaState> schemas = new LinkedHashMap<>();
    }

    public static class SchemaState {
        public Map<String, TableState> tables = new LinkedHashMap<>();
    }

    public static class TableState {
        public String engine;
        public String comment;
        public int shards;
        public List<ColumnState> columns = new ArrayList<>();

        static TableState from(SlothTable table) {
            TableState result = new TableState();
            result.engine = table.getEngineName();
            result.comment = table.getTableComment();
            result.shards = table.getShardNum();
            for (SlothColumn column : table.getColumns()) {
                result.columns.add(ColumnState.from(column));
            }
            return result;
        }

        SlothTable toTable(String tableName, SlothSchema schema) {
            SlothTable table = new SlothTable(tableName);
            table.setSchema(schema);
            table.setEngineName(engine);
            table.setTableComment(comment);
            table.setShardNum(shards);
            List<SlothColumn> restoredColumns = new ArrayList<>();
            for (ColumnState column : columns) {
                restoredColumns.add(column.toColumn());
            }
            table.setColumns(restoredColumns);
            return table;
        }
    }

    public static class ColumnState {
        public String name;
        public String type;
        public boolean unsigned;
        public boolean nullable;
        public String defaultValue;
        public String comment;
        public int precision;

        static ColumnState from(SlothColumn column) {
            EnhanceSlothColumn definition = column.getColumnType();
            ColumnState result = new ColumnState();
            result.name = column.getColumnName();
            result.type = definition.getColumnType().name();
            result.unsigned = definition.isUnsigned();
            result.nullable = definition.isNullable();
            result.defaultValue = definition.getDefalutValue();
            result.comment = definition.getColumnComment();
            result.precision = definition.getPrecision();
            return result;
        }

        SlothColumn toColumn() {
            EnhanceSlothColumn definition = new EnhanceSlothColumn();
            definition.setColumName(name);
            definition.setColumnType(SqlTypeName.valueOf(type));
            definition.setUnsigned(unsigned);
            definition.setNullable(nullable);
            definition.setDefalutValue(defaultValue);
            definition.setColumnComment(comment);
            definition.setPrecision(precision);
            return new SlothColumn(name, definition);
        }
    }
}
