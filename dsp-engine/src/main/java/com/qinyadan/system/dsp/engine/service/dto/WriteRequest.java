package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable statement-level write request.
 */
public final class WriteRequest {

    private final String database;
    private final String table;
    private final List<List<Value>> rows;
    private final String idempotencyKey;

    public WriteRequest(String database, String table, List<List<Value>> rows,
                        String idempotencyKey) {
        this.database = database;
        this.table = table;
        List<List<Value>> copiedRows = null;
        if (rows != null) {
            copiedRows = new ArrayList<>();
            for (List<Value> row : rows) {
                copiedRows.add(row == null ? null
                        : Collections.unmodifiableList(new ArrayList<>(row)));
            }
        }
        this.rows = copiedRows == null ? null : Collections.unmodifiableList(copiedRows);
        this.idempotencyKey = idempotencyKey;
    }

    public String getDatabase() { return database; }
    public String getTable() { return table; }
    public List<List<Value>> getRows() { return rows; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
