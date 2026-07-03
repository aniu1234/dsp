package com.qinyadan.system.dsp.storage.api.meta;

import lombok.Value;

import java.util.List;

/**
 * Request to create a new table.
 */
@Value
public class CreateTableRequest {
    String schema;
    String tableName;
    String engine;
    int shardNum;
    String comment;
    List<ColumnInfo> columns;
}
