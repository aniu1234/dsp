package com.qinyadan.system.dsp.storage.api.meta;

import lombok.Value;

import java.util.List;

/**
 * Request to open an existing table.
 */
@Value
public class OpenTableRequest {
    String schema;
    String tableName;
    List<ColumnInfo> columns;
}
