package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.constant.StringConstants;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.WriteService;
import com.qinyadan.system.dsp.engine.service.dto.WriteColumn;
import com.qinyadan.system.dsp.engine.service.dto.WriteTable;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.storage.api.WriteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.*;


@Slf4j
public class SqlInsertHandler implements Handler<SqlInsert> {

    public static final SqlInsertHandler INSTANCE = new SqlInsertHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlInsert type) {

        final SqlNode table = type.getTargetTable();
        final SqlNodeList columnList = type.getTargetColumnList();

        // INSERT ... SELECT is deliberately rejected until it can provide the
        // same validation and durability guarantees as VALUES inserts.
        if (!(type.getSource() instanceof SqlBasicCall)
                || ((SqlBasicCall) type.getSource()).getOperator()
                != SqlStdOperatorTable.VALUES) {
            writeUnsupportedInsertSource(connectionContext);
            return;
        }
        final List<SqlNode> operands = ((SqlBasicCall) type.getSource()).getOperandList();
        int maxRows = WriteService.INSTANCE.maximumRowsPerInsert();
        if (operands.size() > maxRows) {
            connectionContext.write(PackageUtils.buildErrPackage(
                    WRITE_RESOURCE_LIMIT.getCode(),
                    String.format(WRITE_RESOURCE_LIMIT.getMessage(),
                            "INSERT contains " + operands.size() + " rows; limit is " + maxRows)));
            return;
        }

        final String tableAndDb = table.toString();
        final Pair<String, String> dbAndTablePair =
                StringUtil.getDbAndTablePair(tableAndDb, connectionContext.getDb());
        final String db = dbAndTablePair.getLeft();
        final String tableName = dbAndTablePair.getRight();

        final WriteTable writeTable = checkTableAndDb(connectionContext, db, tableName);
        if (Objects.isNull(writeTable)) {
            return;
        }

        final List<String> columnsNames = checkAndGetColumnName(
                connectionContext, writeTable, columnList);
        if (Objects.isNull(columnsNames)) {
            return;
        }

        final List<List<Value>> valueList = extractColumnValue(connectionContext, operands,
                writeTable, columnsNames);

        if (Objects.isNull(valueList)) {
            return;
        }

        WriteResult writeResult = WriteService.INSTANCE.insert(db, tableName, valueList);

        final MysqlPackage r = PackageUtils.buildOkMySqlPackage(
                writeResult.getInserted(), 1, 0);
        connectionContext.write(r);
    }

    private WriteTable checkTableAndDb(ConnectionContext connectionContext,
                                       String db, String tableName) {
        MysqlPackage r;

        //check whether has used db
        if (Objects.isNull(db)) {
            r = PackageUtils.buildErrPackage(NO_DATABASE_SELECTED.getCode(), NO_DATABASE_SELECTED.getMessage());
            connectionContext.write(r);
            return null;
        }

        //check whether db exists
        if (!CatalogService.INSTANCE.databaseExists(db)) {
            r = PackageUtils.buildErrPackage(UNKNOWN_DB_NAME.getCode(),
                    String.format(UNKNOWN_DB_NAME.getMessage(), db));
            connectionContext.write(r);
            return null;
        }

        //check whether table exists
        final WriteTable writeTable = WriteService.INSTANCE.describe(db, tableName);
        if (Objects.isNull(writeTable)) {
            r = PackageUtils.buildErrPackage(
                    TABLE_NOT_EXISTS.getCode(),
                    String.format(TABLE_NOT_EXISTS.getMessage(), db + StringConstants.DOT + tableName));
            connectionContext.write(r);
            return null;
        }

        return writeTable;
    }

    private List<String> checkAndGetColumnName(ConnectionContext connectionContext,
                                               WriteTable writeTable, SqlNodeList sqlNodes) {

        //insert to t values()....., do not check column name and size
        final List<String> allColumnNames = writeTable.getColumnNames();
        if (Objects.isNull(sqlNodes)) {
            return allColumnNames;
        }

        List<String> resolvedNames = new ArrayList<>(sqlNodes.size());
        Set<String> seen = new HashSet<>();
        for (SqlNode sqlNode : sqlNodes) {
            String requestedName = sqlNode instanceof SqlIdentifier
                    && ((SqlIdentifier) sqlNode).isSimple()
                    ? ((SqlIdentifier) sqlNode).getSimple() : sqlNode.toString();
            WriteColumn column = writeTable.getColumn(requestedName);
            if (column == null) {
                connectionContext.write(PackageUtils.buildErrPackage(
                        UNKONW_COLUMN_NAME.getCode(),
                        String.format(UNKONW_COLUMN_NAME.getMessage(), requestedName)));
                return null;
            }
            String canonicalName = column.getName();
            if (!seen.add(canonicalName.toLowerCase(Locale.ROOT))) {
                connectionContext.write(PackageUtils.buildErrPackage(
                        COLUMN_EXIST_TWICE.getCode(),
                        String.format(COLUMN_EXIST_TWICE.getMessage(), requestedName)));
                return null;
            }
            resolvedNames.add(canonicalName);
        }
        return resolvedNames;
    }

    private List<List<Value>> extractColumnValue(ConnectionContext connectionContext,
                                                 List<SqlNode> rows,
                                                 WriteTable writeTable,
                                                 List<String> columnList) {

        final List<List<Value>> rs = new ArrayList<>();

        final List<String> allColumns = writeTable.getColumnNames();

        int rowCount = rows.size();
        for (int i = 0; i < rowCount; i++) {
            if (!(rows.get(i) instanceof SqlBasicCall)) {
                writeUnsupportedInsertSource(connectionContext);
                return null;
            }
            SqlBasicCall basicCall = (SqlBasicCall) rows.get(i);

            //do not support insert into select syntax
            if (basicCall.getOperator() != SqlStdOperatorTable.ROW) {
                writeUnsupportedInsertSource(connectionContext);
                return null;
            }

            final List<SqlNode> rowValues = basicCall.getOperandList();
            int dataColumnSize = rowValues.size();
            int columnSize = columnList.size();

            if (dataColumnSize != columnSize) {
                MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                        COLUMN_COUNT_NOT_MATCH.getCode(),
                        String.format(COLUMN_COUNT_NOT_MATCH.getMessage(), i + 1));
                connectionContext.write(mysqlPackage);
                return null;
            }

            final Map<String, Value> columnNameAndValue = new HashMap<>();
            for (int j = 0; j < columnSize; j++) {
                final String colName = columnList.get(j);
                final WriteColumn column = writeTable.getColumn(colName);
                final DataType dataType = column.getDataType();

                SqlNode node = rowValues.get(j);
                Value v;

                try {
                    if (node.getKind() == SqlKind.DEFAULT) {
                        v = defaultValue(column, dataType);
                    } else {
                        v = literalValue(node, dataType);
                    }
                    validateColumnValue(column, v);
                } catch (UnsupportedOperationException e) {
                    log.warn("Unsupported INSERT expression: {}", node);
                    connectionContext.write(PackageUtils.buildSyntaxErrPackage(
                            connectionContext.getQueryString()));
                    return null;
                } catch (RuntimeException e) {
                    writeIncorrectValue(connectionContext, node.toString(), colName, i + 1, e);
                    return null;
                }

                columnNameAndValue.put(colName, v);
            }

            List<Value> row = new ArrayList<>();
            for (String columnName : allColumns) {
                Value v = columnNameAndValue.get(columnName);
                if (Objects.isNull(v)) {
                    WriteColumn column = writeTable.getColumn(columnName);
                    DataType dataType = column.getDataType();
                    try {
                        v = defaultValue(column, dataType);
                        validateColumnValue(column, v);
                    } catch (IllegalArgumentException e) {
                        writeIncorrectValue(connectionContext,
                                column.getDefaultValue(), columnName, i + 1, e);
                        return null;
                    }
                }

                row.add(v);
            }

            rs.add(row);
        }

        return rs;
    }

    private void writeUnsupportedInsertSource(ConnectionContext connectionContext) {
        connectionContext.write(PackageUtils.buildErrPackage(
                UNSUPPORTED_FEATURE.getCode(),
                String.format(UNSUPPORTED_FEATURE.getMessage(), "INSERT ... SELECT")));
    }

    private Value defaultValue(WriteColumn column, DataType dataType) {
        String defaultValue = column.getDefaultValue();
        return dataType.createByType(defaultValue);
    }

    private Value literalValue(SqlNode node, DataType dataType) {
        if (SqlUtil.isNullLiteral(node, false)) {
            return dataType.createByType(null);
        }

        BigDecimal number = numericLiteral(node);
        if (number != null) {
            switch (dataType.precedence()) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    // Avoid silently truncating a fractional literal before range checks.
                    return dataType.createByType(number.longValueExact());
                default:
                    return dataType.createByType(number);
            }
        }
        if (node instanceof SqlCharStringLiteral) {
            return dataType.createByType(
                    ((SqlCharStringLiteral) node).getNlsString().getValue());
        }
        if (node instanceof SqlLiteral) {
            return dataType.createByType(((SqlLiteral) node).getValue());
        }
        throw new UnsupportedOperationException("Unsupported INSERT expression: " + node);
    }

    private BigDecimal numericLiteral(SqlNode node) {
        if (node instanceof SqlNumericLiteral) {
            return (BigDecimal) ((SqlNumericLiteral) node).getValue();
        }
        if (!(node instanceof SqlCall)
                || node.getKind() != SqlKind.MINUS_PREFIX
                && node.getKind() != SqlKind.PLUS_PREFIX) {
            return null;
        }
        SqlNode operand = ((SqlCall) node).operand(0);
        if (!(operand instanceof SqlNumericLiteral)) {
            return null;
        }
        BigDecimal value = (BigDecimal) ((SqlNumericLiteral) operand).getValue();
        return node.getKind() == SqlKind.MINUS_PREFIX ? value.negate() : value;
    }

    private void validateColumnValue(WriteColumn column, Value value) {
        if (value.isNull() && !column.isNullable()) {
            throw new IllegalArgumentException(String.format(
                    COLUMN_CANNOT_BE_NULL.getMessage(), column.getName()));
        }
        if (value.isNull()) {
            return;
        }
        Object raw = value.getValueByType();
        if (column.isUnsigned() && raw instanceof Number
                && new BigDecimal(raw.toString()).signum() < 0) {
            throw new IllegalArgumentException("Unsigned column cannot contain a negative value");
        }
        if (column.getPrecision() > 0 && raw instanceof String) {
            String string = (String) raw;
            int characters = string.codePointCount(0, string.length());
            if (characters > column.getPrecision()) {
                throw new IllegalArgumentException("Value exceeds declared column precision");
            }
        }
    }

    private void writeIncorrectValue(ConnectionContext connectionContext, String value,
                                     String column, int row, RuntimeException cause) {
        MysqlPackage mysqlPackage;
        if (cause.getMessage() != null && cause.getMessage().contains("cannot be null")) {
            mysqlPackage = PackageUtils.buildErrPackage(
                    COLUMN_CANNOT_BE_NULL.getCode(),
                    String.format(COLUMN_CANNOT_BE_NULL.getMessage(), column));
        } else {
            mysqlPackage = PackageUtils.buildErrPackage(
                    INCORRECT_COLUMN_VALUE.getCode(),
                    String.format(INCORRECT_COLUMN_VALUE.getMessage(), value, column, row));
        }
        connectionContext.write(mysqlPackage);
    }

}
