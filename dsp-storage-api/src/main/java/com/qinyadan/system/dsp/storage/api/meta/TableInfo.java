package com.qinyadan.system.dsp.storage.api.meta;

import lombok.Value;

import java.util.List;

/**
 * Immutable description of a table.
 */
@Value
public class TableInfo {
    String catalog;
    String schema;
    String name;
    String engine;
    int shards;
    String comment;
    List<ColumnInfo> columns;
}
