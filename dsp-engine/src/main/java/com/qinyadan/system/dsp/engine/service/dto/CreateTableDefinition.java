package com.qinyadan.system.dsp.engine.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable table definition accepted by {@code CatalogService}.
 */
public final class CreateTableDefinition {

    private final String database;
    private final String table;
    private final List<CreateColumnDefinition> columns;
    private final int shards;
    private final String engine;
    private final String comment;

    public CreateTableDefinition(String database, String table,
                                 List<CreateColumnDefinition> columns,
                                 int shards, String engine, String comment) {
        if (database == null || database.trim().isEmpty()
                || table == null || table.trim().isEmpty()
                || columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Database, table and at least one column are required");
        }
        if (shards < 1) {
            throw new IllegalArgumentException("Table must have at least one shard");
        }
        this.database = database;
        this.table = table;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.shards = shards;
        this.engine = engine;
        this.comment = comment;
    }

    public String getDatabase() {
        return database;
    }

    public String getTable() {
        return table;
    }

    public List<CreateColumnDefinition> getColumns() {
        return columns;
    }

    public int getShards() {
        return shards;
    }

    public String getEngine() {
        return engine;
    }

    public String getComment() {
        return comment;
    }
}
