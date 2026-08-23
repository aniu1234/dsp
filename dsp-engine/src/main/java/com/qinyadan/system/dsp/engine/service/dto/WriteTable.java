package com.qinyadan.system.dsp.engine.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only table shape used by protocol adapters to prepare a write request.
 */
public final class WriteTable {

    private final String database;
    private final String table;
    private final List<WriteColumn> columns;
    private final Map<String, WriteColumn> columnsByCanonicalName;

    public WriteTable(String database, String table, List<WriteColumn> columns) {
        if (database == null || table == null || columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Database, table and columns are required");
        }
        this.database = database;
        this.table = table;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        Map<String, WriteColumn> canonical = new LinkedHashMap<>();
        for (WriteColumn column : columns) {
            String key = column.getName().toLowerCase(Locale.ROOT);
            if (canonical.put(key, column) != null) {
                throw new IllegalArgumentException(
                        "Duplicate write column: " + column.getName());
            }
        }
        this.columnsByCanonicalName = Collections.unmodifiableMap(canonical);
    }

    public String getDatabase() {
        return database;
    }

    public String getTable() {
        return table;
    }

    public List<WriteColumn> getColumns() {
        return columns;
    }

    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>(columns.size());
        for (WriteColumn column : columns) {
            result.add(column.getName());
        }
        return Collections.unmodifiableList(result);
    }

    public WriteColumn getColumn(String name) {
        return name == null ? null
                : columnsByCanonicalName.get(name.toLowerCase(Locale.ROOT));
    }
}
