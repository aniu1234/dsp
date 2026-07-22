package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlInsert;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlUtil;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class InsertExecutor {

    int execute(SqlInsert insert, String currentDatabase) {
        if (!(insert.getSource() instanceof SqlBasicCall)) {
            throw failure(InsertException.Reason.UNSUPPORTED_SOURCE, null, null, 0, null);
        }

        QualifiedTable target = qualify(insert.getTargetTable().toString(), currentDatabase);
        if (target.database == null) {
            throw failure(InsertException.Reason.SCHEMA_NOT_FOUND, null, null, 0, null);
        }
        SlothSchema schema = SlothSchemaHolder.INSTANCE.getSlothSchema(target.database);
        if (schema == null) {
            throw failure(InsertException.Reason.SCHEMA_NOT_FOUND,
                    target.database, null, 0, null);
        }
        SlothTable table = (SlothTable) schema.getTable(target.table);
        if (table == null) {
            throw failure(InsertException.Reason.TABLE_NOT_FOUND,
                    target.database + "." + target.table, null, 0, null);
        }

        SlothTableEngine tableEngine = table.getSlothTableEngine();
        List<String> targetColumns = targetColumns(insert.getTargetColumnList(), tableEngine);
        List<SqlNode> rows = ((SqlBasicCall) insert.getSource()).getOperandList();
        List<List<Value>> values = extractRows(rows, tableEngine, targetColumns);
        tableEngine.insert(values);
        return values.size();
    }

    private List<String> targetColumns(SqlNodeList requested, SlothTableEngine tableEngine) {
        List<String> allColumns = tableEngine.getColumnNames();
        if (requested == null) {
            return new ArrayList<>(allColumns);
        }

        List<String> columns = new ArrayList<>(requested.size());
        Set<String> seen = new HashSet<>();
        for (SqlNode node : requested) {
            String column = node.toString();
            if (!allColumns.contains(column)) {
                throw failure(InsertException.Reason.UNKNOWN_COLUMN, column, null, 0, null);
            }
            if (!seen.add(column)) {
                throw failure(InsertException.Reason.DUPLICATE_COLUMN, column, null, 0, null);
            }
            columns.add(column);
        }
        return columns;
    }

    private List<List<Value>> extractRows(List<SqlNode> rows, SlothTableEngine tableEngine,
                                          List<String> targetColumns) {
        List<List<Value>> result = new ArrayList<>(rows.size());
        List<String> allColumns = tableEngine.getColumnNames();
        Map<String, DataType> types = tableEngine.getColumnAndDataType();
        Map<String, SlothColumn> definitions = new HashMap<>();
        for (SlothColumn column : tableEngine.getSlothTable().getColumns()) {
            definitions.put(column.getColumnName(), column);
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            SqlNode rowNode = rows.get(rowIndex);
            if (!(rowNode instanceof SqlBasicCall)
                    || ((SqlBasicCall) rowNode).getOperator() != SqlStdOperatorTable.ROW) {
                throw failure(InsertException.Reason.UNSUPPORTED_SOURCE, null, null, 0, null);
            }
            List<SqlNode> rowValues = ((SqlBasicCall) rowNode).getOperandList();
            if (rowValues.size() != targetColumns.size()) {
                throw failure(InsertException.Reason.COLUMN_COUNT_MISMATCH,
                        null, null, rowIndex + 1, null);
            }

            Map<String, Value> provided = new HashMap<>();
            for (int columnIndex = 0; columnIndex < targetColumns.size(); columnIndex++) {
                String columnName = targetColumns.get(columnIndex);
                SqlNode node = rowValues.get(columnIndex);
                SlothColumn definition = definitions.get(columnName);
                DataType type = types.get(columnName);
                provided.put(columnName,
                        literalValue(node, definition, type, rowIndex + 1));
            }

            List<Value> completeRow = new ArrayList<>(allColumns.size());
            for (String columnName : allColumns) {
                Value value = provided.get(columnName);
                if (value == null) {
                    SlothColumn definition = definitions.get(columnName);
                    value = defaultValue(definition, types.get(columnName), rowIndex + 1);
                }
                completeRow.add(value);
            }
            result.add(completeRow);
        }
        return result;
    }

    private Value literalValue(SqlNode node, SlothColumn column, DataType type, int row) {
        try {
            Value value;
            if (node.getKind() == SqlKind.DEFAULT) {
                value = defaultValue(column, type, row);
            } else if (SqlUtil.isNullLiteral(node, false)) {
                value = type.createByType(null);
            } else if (node instanceof SqlNumericLiteral) {
                value = type.createByType((BigDecimal) ((SqlNumericLiteral) node).getValue());
            } else if (node instanceof SqlCharStringLiteral) {
                value = type.createByType(
                        ((SqlCharStringLiteral) node).getNlsString().getValue());
            } else if (node instanceof SqlLiteral) {
                value = type.createByType(((SqlLiteral) node).getValue());
            } else {
                throw failure(InsertException.Reason.UNSUPPORTED_EXPRESSION,
                        column.getColumnName(), node.toString(), row, null);
            }
            validate(column, value, node.toString(), row);
            return value;
        } catch (InsertException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw failure(InsertException.Reason.INVALID_VALUE,
                    column.getColumnName(), node.toString(), row, e);
        }
    }

    private Value defaultValue(SlothColumn column, DataType type, int row) {
        String raw = column.getColumnType().getDefalutValue();
        try {
            Value value = type.createByType(raw);
            validate(column, value, raw, row);
            return value;
        } catch (InsertException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw failure(InsertException.Reason.INVALID_VALUE,
                    column.getColumnName(), raw, row, e);
        }
    }

    private void validate(SlothColumn column, Value value, String raw, int row) {
        EnhanceSlothColumn definition = column.getColumnType();
        if (value.isNull() && !definition.isNullable()) {
            throw failure(InsertException.Reason.COLUMN_CANNOT_BE_NULL,
                    column.getColumnName(), raw, row, null);
        }
        if (value.isNull()) {
            return;
        }
        Object converted = value.getValueByType();
        if (definition.isUnsigned() && converted instanceof Number
                && new BigDecimal(converted.toString()).signum() < 0) {
            throw failure(InsertException.Reason.INVALID_VALUE,
                    column.getColumnName(), raw, row, null);
        }
        if (definition.getPrecision() > 0 && converted instanceof String
                && ((String) converted).length() > definition.getPrecision()) {
            throw failure(InsertException.Reason.INVALID_VALUE,
                    column.getColumnName(), raw, row, null);
        }
    }

    private QualifiedTable qualify(String table, String currentDatabase) {
        String[] parts = table.split("\\.", 2);
        return parts.length == 1
                ? new QualifiedTable(currentDatabase, parts[0])
                : new QualifiedTable(parts[0], parts[1]);
    }

    private InsertException failure(InsertException.Reason reason, String name,
                                    String value, int row, Throwable cause) {
        return new InsertException(reason, name, value, row, cause);
    }

    private static final class QualifiedTable {
        private final String database;
        private final String table;

        private QualifiedTable(String database, String table) {
            this.database = database;
            this.table = table;
        }
    }
}
