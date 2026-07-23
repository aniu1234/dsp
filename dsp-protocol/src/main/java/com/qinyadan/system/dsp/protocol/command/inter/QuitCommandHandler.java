package com.qinyadan.system.dsp.protocol.command.inter;

import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理client quit/exit
 */
@Slf4j
public class QuitCommandHandler extends AbstractCommandHandler {

    public QuitCommandHandler(ConnectionContext connectionContext) {
        super(connectionContext);
    }

    @Override
    public void execute() {
        log.info("client quit {}", connectionContext.getChannelHandlerContext().channel());
        connectionContext.getChannelHandlerContext().close();
    }
}
