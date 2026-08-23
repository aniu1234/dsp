package com.qinyadan.system.dsp.engine.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Framework-neutral catalog table response.
 */
public final class TableDescription {

    private final String database;
    private final String table;
    private final List<ColumnDescription> columns;
    private final String engine;
    private final int shards;
    private final String comment;

    public TableDescription(String database, String table,
                            List<ColumnDescription> columns, String engine,
                            int shards, String comment) {
        this.database = database;
        this.table = table;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.engine = engine;
        this.shards = shards;
        this.comment = comment;
    }

    public String getDatabase() { return database; }
    public String getTable() { return table; }
    public List<ColumnDescription> getColumns() { return columns; }
    public String getEngine() { return engine; }
    public int getShards() { return shards; }
    public String getComment() { return comment; }
}
