package com.qinyadan.system.dsp.protocol.command.inter;

import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.storage.parser.SlothSchemaHolder;
import io.netty.buffer.ByteBuf;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNKNOWN_DB_NAME;


public class UseDatabaseCommandHandler extends AbstractCommandHandler {

    private final String command;

    public UseDatabaseCommandHandler(ConnectionContext connectionContext, String command) {
        super(connectionContext);
        this.command = command;
    }

    @Override
    public void execute() {
        if (SlothSchemaHolder.INSTANCE.getAllSchemas().contains(command)) {
            connectionContext.setDb(command);
            MysqlPackage mysqlPackage = PackageUtils.buildOkMySqlPackage(0, 1, 0);
            ByteBuf byteBuf = PackageUtils.packageToBuf(mysqlPackage);
            connectionContext.write(byteBuf);
        } else {
            final MysqlPackage mysqlPackage = PackageUtils.buildErrPackage(
                    UNKNOWN_DB_NAME.getCode(),
                    String.format(UNKNOWN_DB_NAME.getMessage(), command));
            connectionContext.write(mysqlPackage);
        }
    }
}
