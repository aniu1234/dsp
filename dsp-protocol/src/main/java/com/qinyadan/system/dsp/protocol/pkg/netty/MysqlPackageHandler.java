package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.qinyadan.system.dsp.protocol.command.CommandHandler;
import com.qinyadan.system.dsp.protocol.command.inter.QueryCommandHandler;
import com.qinyadan.system.dsp.protocol.command.inter.QuitCommandHandler;
import com.qinyadan.system.dsp.protocol.command.inter.PingCommandHandler;
import com.qinyadan.system.dsp.protocol.command.inter.UnsupportedCommandHandler;
import com.qinyadan.system.dsp.protocol.command.inter.UseDatabaseCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.request.Command;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static com.qinyadan.system.dsp.protocol.pkg.MysqlPackage.Protocol.*;


public class MysqlPackageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final MysqlPackage mysqlPackage = (MysqlPackage) msg;
        final ConnectionContext connectionContext = NettyConnectionHandler.INSTANCE
                .getAlreadyAuthenChannels().get(ctx.channel());

        // 消息执行部分是否放业务线程池执行？
        dispatchCommandPackage((Command) mysqlPackage.getAbstractReaderAndWriterPackage(), connectionContext);
    }

    /**
     * mysql协议解析执行？ TODO 通用协议解析执行 mysql ansiSQL hiveSQL
     *
     * @param commandPackage
     * @param connectionContext
     */
    private void dispatchCommandPackage(Command commandPackage, ConnectionContext connectionContext) {
        final byte type = commandPackage.getCommandType();
        CommandHandler handler;
        switch (type) {
            case COM_QUERY:
                handler = new QueryCommandHandler(connectionContext, commandPackage.getCommand());
                break;
            case COM_INIT_DB:
                handler = new UseDatabaseCommandHandler(connectionContext, commandPackage.getCommand());
                break;
            case COM_QUIT:
                // 需要退出登录态等
                handler = new QuitCommandHandler(connectionContext);
                break;
            case COM_PING:
                handler = new PingCommandHandler(connectionContext);
                break;
            default:
                handler = new UnsupportedCommandHandler(connectionContext, type);
        }

        handler.execute();
    }
}
