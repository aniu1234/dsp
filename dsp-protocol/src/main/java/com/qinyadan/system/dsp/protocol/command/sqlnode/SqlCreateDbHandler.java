package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlCreateDb;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.DATABASE_EXISTS_ERROR;


public class SqlCreateDbHandler implements Handler<SqlCreateDb> {

    public static final SqlCreateDbHandler INSTANCE = new SqlCreateDbHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlCreateDb type) {
        final String db = type.getDbName();

        //db already exists
        if (CatalogService.INSTANCE.databaseExists(db)) {
            MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                    DATABASE_EXISTS_ERROR.getCode(),
                    String.format(DATABASE_EXISTS_ERROR.getMessage(), db));

            connectionContext.write(mysqlPackage);
            return;
        }

        CatalogService.INSTANCE.createDatabase(db);
        final MysqlPackage mysqlPackage = PackageUtils.buildOkMySqlPackage(1, 1, 0);
        connectionContext.write(mysqlPackage);
    }
}
