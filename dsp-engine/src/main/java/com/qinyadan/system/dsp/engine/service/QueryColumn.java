package com.qinyadan.system.dsp.engine.service;

import java.util.Objects;

/**
 * Stable query-column metadata exposed to protocol adapters.
 */
public final class QueryColumn {

    private final String name;
    private final int jdbcType;

    public QueryColumn(String name, int jdbcType) {
        this.name = Objects.requireNonNull(name, "name");
        this.jdbcType = jdbcType;
    }

    public String getName() {
        return name;
    }

    public int getJdbcType() {
        return jdbcType;
    }
}
