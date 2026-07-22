package com.qinyadan.system.dsp.protocol.command.inter;

import com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum;
import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;

public class UnsupportedCommandHandler extends AbstractCommandHandler {

    private final int commandType;

    public UnsupportedCommandHandler(ConnectionContext connectionContext, byte commandType) {
        super(connectionContext);
        this.commandType = commandType & 0xff;
    }

    @Override
    public void execute() {
        connectionContext.write(PackageUtils.buildErrPackage(
                ErrorCodeAndMessageEnum.UNKNOWN_COMMAND.getCode(),
                ErrorCodeAndMessageEnum.UNKNOWN_COMMAND.getMessage() + ": " + commandType));
    }
}
