package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.engine.service.dto.WriteColumn;
import com.qinyadan.system.dsp.engine.service.dto.WriteTable;
import com.qinyadan.system.dsp.storage.api.WriteResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Application boundary for validated statement-level table writes.
 */
public final class WriteService {

    private static final int DEFAULT_MAX_ROWS_PER_INSERT = 10_000;

    public static final WriteService INSTANCE = new WriteService();

    private WriteService() {
    }

    public WriteTable describe(String database, String tableName) {
        SlothTable table = CatalogService.INSTANCE.getTable(database, tableName);
        if (table == null || table.getSlothTableEngine() == null) {
            return null;
        }
        return describe(database, tableName, table);
    }

    private WriteTable describe(String database, String tableName, SlothTable table) {
        SlothTableEngine engine = table.getSlothTableEngine();
        List<WriteColumn> columns = new ArrayList<>(table.getColumns().size());
        for (SlothColumn column : table.getColumns()) {
            EnhanceSlothColumn definition = column.getColumnType();
            columns.add(new WriteColumn(column.getColumnName(),
                    engine.getColumnAndDataType().get(column.getColumnName()),
                    definition.isNullable(), definition.isUnsigned(),
                    definition.getPrecision(), definition.getDefalutValue()));
        }
        return new WriteTable(database, tableName, columns);
    }

    public WriteResult insert(String database, String tableName,
                              List<List<Value>> rows) {
        SlothTable slothTable = CatalogService.INSTANCE.getTable(database, tableName);
        if (slothTable == null || slothTable.getSlothTableEngine() == null) {
            throw new IllegalArgumentException(
                    "Unknown table: " + database + "." + tableName);
        }
        validate(describe(database, tableName, slothTable), rows);
        return slothTable.getSlothTableEngine().insert(rows);
    }

    public int maximumRowsPerInsert() {
        String configured = System.getProperty("dsp.write.max-rows-per-insert");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("DSP_WRITE_MAX_ROWS_PER_INSERT");
        }
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_MAX_ROWS_PER_INSERT;
        }
        try {
            int value = Integer.parseInt(configured.trim());
            if (value < 1) {
                throw new IllegalArgumentException(
                        "Write row limit must be greater than zero: " + configured);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid write row limit: " + configured, e);
        }
    }

    private void validate(WriteTable table, List<List<Value>> rows) {
        if (rows == null) {
            throw new IllegalArgumentException("Rows to insert must not be null");
        }
        if (rows.size() > maximumRowsPerInsert()) {
            throw new IllegalArgumentException("Write exceeds the configured row limit");
        }
        List<WriteColumn> columns = table.getColumns();
        for (List<Value> row : rows) {
            if (row == null || row.size() != columns.size()) {
                throw new IllegalArgumentException(
                        "Write row does not match the table column count");
            }
            for (int i = 0; i < row.size(); i++) {
                validateValue(columns.get(i), row.get(i));
            }
        }
    }

    private void validateValue(WriteColumn column, Value value) {
        if (value == null || value.isNull()) {
            if (!column.isNullable()) {
                throw new IllegalArgumentException(
                        "Column '" + column.getName() + "' cannot be null");
            }
            return;
        }
        if (!column.getDataType().equals(value.getType())) {
            throw new IllegalArgumentException(
                    "Value type does not match column '" + column.getName() + "'");
        }
        Object raw = value.getValueByType();
        if (column.isUnsigned() && raw instanceof Number
                && new BigDecimal(raw.toString()).signum() < 0) {
            throw new IllegalArgumentException(
                    "Unsigned column cannot contain a negative value");
        }
        if (column.getPrecision() > 0 && raw instanceof String) {
            String string = (String) raw;
            if (string.codePointCount(0, string.length()) > column.getPrecision()) {
                throw new IllegalArgumentException(
                        "Value exceeds declared column precision");
            }
        }
    }
}
