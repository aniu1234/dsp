package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.constant.StringConstants;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (!(type.getSource() instanceof SqlBasicCall)) {
            writeUnsupportedInsertSource(connectionContext);
            return;
        }
        final List<SqlNode> operands = ((SqlBasicCall) type.getSource()).getOperandList();

        final String tableAndDb = table.toString();
        final Pair<String, String> dbAndTablePair =
                StringUtil.getDbAndTablePair(tableAndDb, connectionContext.getDb());
        final String db = dbAndTablePair.getLeft();
        final String tableName = dbAndTablePair.getRight();

        final SlothTable slothTable = checkTableAndDb(connectionContext, db, tableName);
        if (Objects.isNull(slothTable)) {
            return;
        }

        final SlothTableEngine slothTableEngine = slothTable.getSlothTableEngine();
        final List<String> columnsNames = checkAndGetColumnName(connectionContext, slothTableEngine, columnList);
        if (Objects.isNull(columnsNames)) {
            return;
        }

        final List<List<Value>> valueList = extractColumnValue(connectionContext, operands,
                slothTableEngine, columnsNames);

        if (Objects.isNull(valueList)) {
            return;
        }

        slothTableEngine.insert(valueList);

        final MysqlPackage r = PackageUtils.buildOkMySqlPackage(valueList.size(), 1, 0);
        connectionContext.write(r);
    }

    private SlothTable checkTableAndDb(ConnectionContext connectionContext, String db, String tableName) {
        MysqlPackage r;

        //check whether has used db
        if (Objects.isNull(db)) {
            r = PackageUtils.buildErrPackage(NO_DATABASE_SELECTED.getCode(), NO_DATABASE_SELECTED.getMessage());
            connectionContext.write(r);
            return null;
        }

        //check whether db exists
        final SlothSchema slothSchema = SlothSchemaHolder.INSTANCE.getSlothSchema(db);
        if (Objects.isNull(slothSchema)) {
            r = PackageUtils.buildErrPackage(NO_DATABASE_SELECTED.getCode(), NO_DATABASE_SELECTED.getMessage());
            connectionContext.write(r);
            return null;
        }

        //check whether table exists
        final SlothTable slothTable = (SlothTable) slothSchema.getTable(tableName);
        if (Objects.isNull(slothTable)) {
            r = PackageUtils.buildErrPackage(
                    TABLE_NOT_EXISTS.getCode(),
                    String.format(TABLE_NOT_EXISTS.getMessage(), db + StringConstants.DOT + tableName));
            connectionContext.write(r);
            return null;
        }

        return slothTable;
    }

    private List<String> checkAndGetColumnName(ConnectionContext connectionContext,
                                               SlothTableEngine slothTableEngine, SqlNodeList sqlNodes) {

        //insert to t values()....., do not check column name and size
        final List<String> allColumnNames = slothTableEngine.getColumnNames();
        if (Objects.isNull(sqlNodes)) {
            return allColumnNames;
        }

        final List<String> columnsNames = sqlNodes.getList().stream()
                .map(SqlNode::toString)
                .collect(Collectors.toList());


        final List<String> unknowColumns = ListUtils.removeAll(columnsNames, allColumnNames);

        //insert into t(c1, c2) values(1, 2) and c1 does not exsit in table
        if (CollectionUtils.isNotEmpty(unknowColumns)) {
            MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                    UNKONW_COLUMN_NAME.getCode(),
                    String.format(UNKONW_COLUMN_NAME.getMessage(), unknowColumns.toString()));

            connectionContext.write(mysqlPackage);
            return null;
        }

        //insert into t(c1, c2) values(2, 2) c1 exists twice
        final List<Pair<String, Integer>> pairs = columnsNames.stream()
                .collect(Collectors.groupingBy(f -> f))
                .entrySet()
                .stream()
                .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue().size()))
                .filter(immutablePair -> immutablePair.right > 1)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(pairs)) {
            MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                    UNKONW_COLUMN_NAME.getCode(),
                    String.format(COLUMN_EXIST_TWICE.getMessage(), pairs.get(0).getLeft()));

            connectionContext.write(mysqlPackage);
            return null;
        }

        return columnsNames;
    }

    private List<List<Value>> extractColumnValue(ConnectionContext connectionContext,
                                                 List<SqlNode> rows,
                                                 SlothTableEngine slothTableEngine,
                                                 List<String> columnList) {

        final List<List<Value>> rs = Lists.newArrayList();

        final List<String> allColumns = slothTableEngine.getColumnNames();
        final Map<String, DataType> map = slothTableEngine.getColumnAndDataType();
        final Map<String, SlothColumn> definitions = new HashMap<>();
        for (SlothColumn column : slothTableEngine.getSlothTable().getColumns()) {
            definitions.put(column.getColumnName(), column);
        }

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

            final Map<String, Value> columnNameAndValue = Maps.newHashMap();
            for (int j = 0; j < columnSize; j++) {
                final String colName = columnList.get(j);
                final DataType dataType = map.get(colName);
                final SlothColumn column = definitions.get(colName);

                SqlNode node = rowValues.get(j);
                Value v;

                try {
                    if (node.getKind() == SqlKind.DEFAULT) {
                        v = defaultValue(column, dataType);
                    } else if (SqlUtil.isNullLiteral(node, false)) {
                        v = dataType.createByType(null);
                    } else if (node instanceof SqlNumericLiteral) {
                        BigDecimal decimal = (BigDecimal) ((SqlNumericLiteral) node).getValue();
                        v = dataType.createByType(decimal);
                    } else if (node instanceof SqlCharStringLiteral) {
                        SqlCharStringLiteral sqlCharStringLiteral = (SqlCharStringLiteral) node;
                        final String stringValue = sqlCharStringLiteral.getNlsString().getValue();
                        v = dataType.createByType(stringValue);
                    } else if (node instanceof SqlLiteral) {
                        v = dataType.createByType(((SqlLiteral) node).getValue());
                    } else {
                        log.warn("Unsupported INSERT expression: {}", node);
                        MysqlPackage mysqlPackage = PackageUtils.buildSyntaxErrPackage(
                                connectionContext.getQueryString());
                        connectionContext.write(mysqlPackage);
                        return null;
                    }
                    validateColumnValue(column, v);
                } catch (IllegalArgumentException e) {
                    writeIncorrectValue(connectionContext, node.toString(), colName, i + 1, e);
                    return null;
                }

                columnNameAndValue.put(colName, v);
            }

            List<Value> row = Lists.newArrayList();
            for (String columnName : allColumns) {
                Value v = columnNameAndValue.get(columnName);
                if (Objects.isNull(v)) {
                    DataType dataType = map.get(columnName);
                    SlothColumn column = definitions.get(columnName);
                    try {
                        v = defaultValue(column, dataType);
                        validateColumnValue(column, v);
                    } catch (IllegalArgumentException e) {
                        writeIncorrectValue(connectionContext,
                                column.getColumnType().getDefalutValue(), columnName, i + 1, e);
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

    private Value defaultValue(SlothColumn column, DataType dataType) {
        String defaultValue = column.getColumnType().getDefalutValue();
        return dataType.createByType(defaultValue);
    }

    private void validateColumnValue(SlothColumn column, Value value) {
        EnhanceSlothColumn definition = column.getColumnType();
        if (value.isNull() && !definition.isNullable()) {
            throw new IllegalArgumentException(String.format(
                    COLUMN_CANNOT_BE_NULL.getMessage(), column.getColumnName()));
        }
        if (value.isNull()) {
            return;
        }
        Object raw = value.getValueByType();
        if (definition.isUnsigned() && raw instanceof Number
                && new BigDecimal(raw.toString()).signum() < 0) {
            throw new IllegalArgumentException("Unsigned column cannot contain a negative value");
        }
        if (definition.getPrecision() > 0 && raw instanceof String
                && ((String) raw).length() > definition.getPrecision()) {
            throw new IllegalArgumentException("Value exceeds declared column precision");
        }
    }

    private void writeIncorrectValue(ConnectionContext connectionContext, String value,
                                     String column, int row, IllegalArgumentException cause) {
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
