package com.qinyadan.system.dsp.storage.api.meta;

import java.util.List;

/**
 * Interface for managing schema and table metadata.
 */
public interface MetadataStore {
    void createSchema(String schemaName);
    void dropSchema(String schemaName);
    List<String> listSchemas();

    void createTable(CreateTableRequest request);
    void dropTable(String schema, String table);
    TableInfo getTable(String schema, String table);
    List<TableInfo> listTables(String schema);
}
