package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.constant.StringConstants;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.engine.ShowEnum;
import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlShow;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.dto.ColumnDescription;
import com.qinyadan.system.dsp.engine.service.dto.TableDescription;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.qinyadan.system.dsp.constant.ColumnTypeConstants.MYSQL_TYPE_VAR_STRING;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.*;


public class SqlShowHandler implements Handler<SqlShow> {

    public static final SqlShowHandler INSTANCE = new SqlShowHandler();

    private static final String CREATE_TABLE_RESULT_COLUMN1 = "Table";
    private static final String CREATE_TABLE_RESULT_COLUMN2 = "Create Table";

    private static final String SHOW_DATABASE_RESULT_COLUMN = "Database";

    @Override
    public void handle(ConnectionContext connectionContext, SqlShow type) {
        final String command = type.getCommand();

        List<List<String>> data;
        String[] columnName = {SHOW_DATABASE_RESULT_COLUMN};
        final ShowEnum showType = type.getType();
        switch (showType) {
            case SHOW_DBS:
                data = CatalogService.INSTANCE.listDatabases().stream()
                        .map(Lists::newArrayList)
                        .collect(Collectors.toList());
                break;

            case SHOW_TABLBS:
                final String db = connectionContext.getDb();
                if (Objects.isNull(db)) {
                    MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                            NO_DATABASE_SELECTED.getCode(),
                            NO_DATABASE_SELECTED.getMessage());

                    connectionContext.write(mysqlPackage);
                    return;
                }
                columnName = new String[]{String.join(StringConstants.UNDER_LINE,
                        Lists.newArrayList("Tables", "in", db))};

                if (!CatalogService.INSTANCE.databaseExists(db)) {
                    connectionContext.write(PackageUtils.buildErrPackage(
                            UNKNOWN_DB_NAME.getCode(),
                            String.format(UNKNOWN_DB_NAME.getMessage(), db)));
                    return;
                }
                data = CatalogService.INSTANCE.listTables(db).stream()
                        .map(Lists::newArrayList).collect(Collectors.toList());

                break;
            case SHOW_CREATE:
                final ShowCreateTableResult showCreateTableResult = getCreateTable(command, connectionContext.getDb());
                if (showCreateTableResult.hasError) {
                    connectionContext.write(showCreateTableResult.mysqlPackage);
                    return;
                }

                data = Lists.newArrayListWithCapacity(1);
                data.add(showCreateTableResult.columnValues);
                columnName = showCreateTableResult.columnNames;

                break;
            default:
                MysqlPackage r = PackageUtils.buildErrPackage(
                        SYNTAX_ERROR.getCode(),
                        String.format(SYNTAX_ERROR.getMessage(), connectionContext.getQueryString()));

                connectionContext.write(r);
                return;
        }

        final List<Integer> columnTypes = Lists.newArrayList();
        for (int i = 0; i < columnName.length; i++) {
            columnTypes.add(MYSQL_TYPE_VAR_STRING);
        }

        final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                .columnName(columnName)
                .columnType(columnTypes)
                .data(data)
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();

        final ByteBuf byteBuf = PackageUtils.buildResultSet(resultSetHolder);
        connectionContext.write(byteBuf);
    }

    private ShowCreateTableResult getCreateTable(String tableNameDatabase, String dbFromConnction) {
        final Pair<String, String> dbAndTablePair = StringUtil.getDbAndTablePair(tableNameDatabase, dbFromConnction);
        final String db = dbAndTablePair.getLeft();
        final String tableName = dbAndTablePair.getRight();

        MysqlPackage mysqlPackage = null;
        if (Objects.isNull(db)) {
            mysqlPackage = PackageUtils.buildErrPackage(
                    NO_DATABASE_SELECTED.getCode(),
                    NO_DATABASE_SELECTED.getMessage());

            return new ShowCreateTableResult(true, null, null, mysqlPackage);
        }

        if (!CatalogService.INSTANCE.databaseExists(db)) {
            mysqlPackage = PackageUtils.buildErrPackage(
                    TABLE_NOT_EXISTS.getCode(),
                    String.format(TABLE_NOT_EXISTS.getMessage(), tableNameDatabase));
            return new ShowCreateTableResult(true, null, null, mysqlPackage);
        }

        final TableDescription table = CatalogService.INSTANCE.describeTable(db, tableName);
        if (Objects.isNull(table)) {
            mysqlPackage = PackageUtils.buildErrPackage(
                    TABLE_NOT_EXISTS.getCode(),
                    String.format(TABLE_NOT_EXISTS.getMessage(), tableNameDatabase));
            return new ShowCreateTableResult(true, null, null, mysqlPackage);
        }

        //now start to get re
        final String[] columnNames = {CREATE_TABLE_RESULT_COLUMN1, CREATE_TABLE_RESULT_COLUMN2};
        final List<String> datas = Lists.newArrayList(tableName, buildCreateTableSql(table));

        return new ShowCreateTableResult(false, columnNames, datas, mysqlPackage);
    }

    private String buildCreateTableSql(TableDescription table) {

        final StringBuilder builder = new StringBuilder();

        builder.append("CREATE TABLE `").append(table.getTable()).append("` (\n");

        //column
        final List<ColumnDescription> columns = table.getColumns();
        final int length = columns.size();
        for (int i = 0; i < length; i++) {
            final ColumnDescription column = columns.get(i);

            builder.append("  `").append(column.getName()).append("` ");
            builder.append(column.getSqlType().toLowerCase()).append(" ");

            if (column.getPrecision() > 0) {
                builder.append("(").append(column.getPrecision()).append(") ");
            }

            if (column.isUnsigned()) {
                builder.append("unsigned ");
            }

            if (!column.isNullable()) {
                builder.append("NOT NULL ");
            }

            if (Objects.nonNull(column.getDefaultValue())) {
                builder.append("DEFAULT ").append(column.getDefaultValue()).append(" ");
            }

            if (StringUtils.isNotBlank(column.getComment())) {
                builder.append("COMMENT ").append(column.getComment());
            }

            if (i != length - 1) {
                builder.append(",");
            }

            builder.append("\n");
        }

        builder.append(") ");

        //table property
        String engineName = table.getEngine();
        if (Objects.isNull(engineName)) {
            engineName = "lucene";
        }
        builder.append("ENGINE = ").append(engineName).append(" ");

        builder.append("SHARD = ").append(table.getShards()).append(" ");
        final String tableComment = table.getComment();
        if (StringUtils.isNotBlank(tableComment)) {
            builder.append("COMMENT = ").append(tableComment);
        }

        return builder.toString();
    }

    static class ShowCreateTableResult {
        private boolean hasError;
        private String[] columnNames;
        private List<String> columnValues;

        /**
         * not null iff hasError is true
         */
        private MysqlPackage mysqlPackage;

        ShowCreateTableResult(boolean hasError, String[] columnNames, List<String> columnValues, MysqlPackage mysqlPackage) {
            this.hasError = hasError;
            this.columnNames = columnNames;
            this.columnValues = columnValues;
            this.mysqlPackage = mysqlPackage;
        }
    }
}
