package com.qinyadan.system.dsp.storage.api.query;

import lombok.Getter;

import java.util.Set;

@Getter
public class QueryContext {
    private final Query query;
    private final Set<String> columnNames;

    public QueryContext(Query query, Set<String> columnNames) {
        this.query = query;
        this.columnNames = columnNames;
    }
}
