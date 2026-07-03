package com.qinyadan.system.dsp.storage.api.query;

import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
public class BaseQuery implements Query {
    private String query;
    private Set<String> columnNames;

    @Override
    public String getSql() {
        return query;
    }

    @Override
    public Set<String> getColumnNames() {
        return columnNames != null ? columnNames : new HashSet<>();
    }
}
