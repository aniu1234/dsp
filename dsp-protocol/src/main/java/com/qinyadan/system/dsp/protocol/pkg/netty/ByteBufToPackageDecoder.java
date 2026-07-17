package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.auth.LoginRequest;
import com.qinyadan.system.dsp.protocol.pkg.request.Command;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class ByteBufToPackageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        final MysqlPackage result = new MysqlPackage();

        if (NettyConnectionHandler.INSTANCE.channelHasAuthencation(channelHandlerContext.channel())) {
            result.setAbstractReaderAndWriterPackage(new Command());
        } else {
            result.setAbstractReaderAndWriterPackage(new LoginRequest());
        }

        result.read(byteBuf);

        log.info("ByteBufToPackageDecoder decode protocol MysqlPackage : {} ", result);
        list.add(result);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Failed to decode MySQL protocol packet from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
