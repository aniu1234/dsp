package com.qinyadan.system.dsp.storage;


import lombok.Getter;

import java.util.Set;


public class QueryContext {
    @Getter
    private final Query query;
    @Getter
    private final Set<String> columnNames;

    public QueryContext(Query query, Set<String> columnNames) {
        this.query = query;
        this.columnNames = columnNames;
    }

}
