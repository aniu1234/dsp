package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlUse;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNKNOWN_DB_NAME;


public class SqlUseHandler implements Handler<SqlUse> {

    public static final SqlUseHandler INSTANCE = new SqlUseHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlUse type) {
        final String db = type.getDb();
        final MysqlPackage mysqlPackage = useDb(connectionContext, db);
        connectionContext.write(mysqlPackage);
    }

    public MysqlPackage useDb(ConnectionContext connectionContext, String db) {
        //check if schema contains db name;
        if (!CatalogService.INSTANCE.databaseExists(db)) {
            return PackageUtils.buildErrPackage(
                    UNKNOWN_DB_NAME.getCode(),
                    String.format(UNKNOWN_DB_NAME.getMessage(), db));
        }

        connectionContext.setDb(db);
        return PackageUtils.buildOkMySqlPackage(0, 1, 0);
    }
}
