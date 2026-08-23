package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.result.ErrorMessage;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.calcite.SlothColumnType;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlCreateTable;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.dto.CreateColumnDefinition;
import com.qinyadan.system.dsp.engine.service.dto.CreateTableDefinition;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.*;


public class SqlCreateTableHandler implements Handler<SqlCreateTable> {

    public static final SqlCreateTableHandler INSTANCE = new SqlCreateTableHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlCreateTable type) {
        final String tableName = type.getTableName();
        final boolean isNotExist = type.isNotExisted();
        final String db = connectionContext.getDb();

        final ErrorMessage errorMessage = checkDbAndTableName(tableName, db, isNotExist);

        if (ErrorMessage.OK_MESSAGE_AND_RETURN == errorMessage) {
            MysqlPackage mysqlPackage = PackageUtils.buildOkMySqlPackage(0, 1, 0);
            connectionContext.write(mysqlPackage);
            return;
        }

        if (ErrorMessage.OK_MESSAGE != errorMessage) {
            MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(errorMessage.getErrorCode(), errorMessage.getDetailMessage());
            connectionContext.write(mysqlPackage);
            return;
        }

        //now handle
        createTable(tableName, type, connectionContext);
    }


    private void createTable(String tableAndDb, SqlCreateTable sqlCreateTable, ConnectionContext connectionContext) {
        final SqlNodeList sqlNodes = sqlCreateTable.getNameAndType();

        final Pair<String, String> dbAndTablePair = StringUtil.getDbAndTablePair(tableAndDb, connectionContext.getDb());
        final String db = dbAndTablePair.getLeft();
        final String tableName = dbAndTablePair.getRight();

        final List<SqlNode> nodes = sqlNodes.getList();
        final int size = nodes.size();

        final List<CreateColumnDefinition> columns = Lists.newArrayList();
        final Set<String> columnNames = new HashSet<>();
        for (int i = 0; i < size / 2; i++) {
            final String columnName = nodes.get(2 * i).toString();
            if (!columnNames.add(columnName)) {
                MysqlPackage error = PackageUtils.buildErrPackage(
                        DUPLICATE_COLUMN_NAME.getCode(),
                        String.format(DUPLICATE_COLUMN_NAME.getMessage(), columnName));
                connectionContext.write(error);
                return;
            }
            final SlothColumnType sqlTypeName = (SlothColumnType) nodes.get(2 * i + 1);
            columns.add(sqlTypeName.toColumnDefinition(columnName));
        }

        String engineName = SlothTable.DEFAULT_ENGINE_NAME;
        if (Objects.nonNull(sqlCreateTable.getEngine())) {
            engineName = sqlCreateTable.getEngine();
        }
        String tableComment = null;
        if (Objects.nonNull(sqlCreateTable.getTableComment())) {
            tableComment = sqlCreateTable.getTableComment().toString();
        }
        CatalogService.INSTANCE.createTable(new CreateTableDefinition(
                db, tableName, columns, sqlCreateTable.getShard(),
                engineName, tableComment));

        final MysqlPackage result =
                PackageUtils.buildOkMySqlPackage(0, 1, 0);
        connectionContext.write(result);
    }


    private ErrorMessage checkDbAndTableName(String tableNameAndDB, String db, boolean isNotExist) {
        final Pair<String, String> dbAndTablePair = StringUtil.getDbAndTablePair(tableNameAndDB, db);

        final String realDb = dbAndTablePair.getLeft();
        final String tableName = dbAndTablePair.getRight();


        if (Objects.isNull(realDb)) {
            return new ErrorMessage(NO_DATABASE_SELECTED.getCode(), NO_DATABASE_SELECTED.getMessage());
        }

        if (!CatalogService.INSTANCE.databaseExists(realDb)) {
            return new ErrorMessage(UNKNOWN_DB_NAME.getCode(), String.format(UNKNOWN_DB_NAME.getMessage(), realDb));
        }

        if (CatalogService.INSTANCE.tableExists(realDb, tableName)) {
            if (isNotExist) {
                return ErrorMessage.OK_MESSAGE_AND_RETURN;
            } else {
                return new ErrorMessage(TABLE_ALREADY_EXISTS.getCode(),
                        String.format(TABLE_ALREADY_EXISTS.getMessage(), tableNameAndDB));
            }
        }

        return ErrorMessage.OK_MESSAGE;
    }
}
