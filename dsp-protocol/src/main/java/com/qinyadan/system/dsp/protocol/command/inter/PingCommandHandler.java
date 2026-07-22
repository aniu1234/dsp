package com.qinyadan.system.dsp.protocol.command.inter;

import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;

public class PingCommandHandler extends AbstractCommandHandler {

    public PingCommandHandler(ConnectionContext connectionContext) {
        super(connectionContext);
    }

    @Override
    public void execute() {
        connectionContext.write(PackageUtils.buildOkMySqlPackage(0, 1, 0));
    }
}
