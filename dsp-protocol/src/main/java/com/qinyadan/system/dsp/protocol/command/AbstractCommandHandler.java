package com.qinyadan.system.dsp.protocol.command;

import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;


public abstract class AbstractCommandHandler implements CommandHandler {

    protected ConnectionContext connectionContext;

    public AbstractCommandHandler(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
}
