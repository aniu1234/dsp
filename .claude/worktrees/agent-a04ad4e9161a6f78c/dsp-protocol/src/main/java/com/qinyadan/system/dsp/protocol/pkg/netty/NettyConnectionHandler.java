package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.protocol.pkg.auth.ServerGreeting;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;


public class NettyConnectionHandler extends ChannelInboundHandlerAdapter {

    public static final NettyConnectionHandler INSTANCE = new NettyConnectionHandler();

    private Map<Channel, ConnectionContext> alreadyAuthenChannels = Maps.newConcurrentMap();

    public Map<Channel, ConnectionContext> getAlreadyAuthenChannels() {
        return alreadyAuthenChannels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //todo, do some log
        Channel channel = ctx.channel();
        sendAuthencationPackage(channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        getAlreadyAuthenChannels().remove(ctx.channel());
    }

    private boolean sendAuthencationPackage(Channel channel) {
        ServerGreeting serverGreetingPackage = PackageUtils.buildInitAuthencatinPackage();
        ByteBuf tmp = PooledByteBufAllocator.DEFAULT.buffer(128);
        serverGreetingPackage.write(tmp);

        int length = tmp.readableBytes();
        byte[] bytes = new byte[length];
        tmp.readBytes(bytes);

        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(128);
        IOUtils.writeInteger3(length, byteBuf);
        IOUtils.writeByte((byte) 0x00, byteBuf);
        byteBuf.writeBytes(bytes);


        byte[] by = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(by);

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(128);
        buf.writeBytes(by);

        channel.writeAndFlush(buf);
        return true;
    }

    public boolean channelHasAuthencation(Channel channel) {
        return alreadyAuthenChannels.containsKey(channel);
    }


}
