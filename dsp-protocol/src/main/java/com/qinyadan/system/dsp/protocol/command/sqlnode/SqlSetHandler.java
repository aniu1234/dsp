package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.engine.service.EnvironmentService;
import com.qinyadan.system.dsp.engine.parser.ddl.SqlSet;


public class SqlSetHandler implements Handler<SqlSet> {

    public static final SqlSetHandler INSTANCE = new SqlSetHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlSet type) {
        final String key = type.getKey();
        final String value = type.getValue();

        final boolean isGlobal = type.isGobal();

        //TODO Currently, we do not check the key and value is a valid key or value;
        if (isGlobal) {
            EnvironmentService.INSTANCE.setGlobal(key, value);
        } else {
            connectionContext.getProperties().put(key, value);
        }

        final MysqlPackage mysqlPackage = PackageUtils.buildOkMySqlPackage(0, 1, 0);
        connectionContext.write(mysqlPackage);
    }
}
