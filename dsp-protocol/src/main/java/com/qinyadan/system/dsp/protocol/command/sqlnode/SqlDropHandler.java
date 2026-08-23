package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.core.util.StringUtil;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlDrop;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.*;


public class SqlDropHandler implements Handler<SqlDrop> {

    public static final SqlDropHandler INSTANCE = new SqlDropHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlDrop type) {
        //handle sql create db;
        final String name = type.getName();
        final boolean isDb = type.isDropDb();

        if (isDb) {
            dropDb(name, connectionContext);
        } else {
            dropTable(name, connectionContext);
        }
    }


    private void dropDb(String dbName, ConnectionContext connectionContext) {
        if (!CatalogService.INSTANCE.databaseExists(dbName)) {
            final MysqlPackage error = PackageUtils.buildErrPackage(
                    DATABASE_NOT_EXIST_IN_DROP.getCode(),
                    DATABASE_NOT_EXIST_IN_DROP.getMessage());

            connectionContext.write(error);
            return;
        }

        CatalogService.INSTANCE.dropDatabase(dbName);

        final MysqlPackage mysqlPackage =
                PackageUtils.buildOkMySqlPackage(1, 1, 0);

        connectionContext.write(mysqlPackage);
    }

    private void dropTable(String tableAndDb, ConnectionContext connectionContext) {
        final Pair<String, String> dbAndTablePair =
                StringUtil.getDbAndTablePair(tableAndDb, connectionContext.getDb());

        final String db = dbAndTablePair.getLeft();
        final String table = dbAndTablePair.getRight();
        if (Objects.isNull(db)) {
            final MysqlPackage error = PackageUtils.buildErrPackage(
                    NO_DATABASE_SELECTED.getCode(),
                    NO_DATABASE_SELECTED.getMessage(), 1);
            connectionContext.write(error);
            return;
        }

        if (!CatalogService.INSTANCE.tableExists(db, table)) {
            final MysqlPackage error = PackageUtils.buildErrPackage(
                    UNKNOWN_TABLE_NAME.getCode(),
                    String.format(UNKNOWN_TABLE_NAME.getMessage(), tableAndDb));
            connectionContext.write(error);
            return;
        }

        CatalogService.INSTANCE.dropTable(db, table);
        final MysqlPackage mysqlPackage =
                PackageUtils.buildOkMySqlPackage(1, 1, 0);

        connectionContext.write(mysqlPackage);
    }
}
