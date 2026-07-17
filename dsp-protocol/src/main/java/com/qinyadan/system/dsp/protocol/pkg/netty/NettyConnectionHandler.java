package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.protocol.pkg.auth.ServerGreeting;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.security.SecureRandom;
import java.util.Map;


@ChannelHandler.Sharable
public class NettyConnectionHandler extends ChannelInboundHandlerAdapter {

    public static final NettyConnectionHandler INSTANCE = new NettyConnectionHandler();

    private Map<Channel, ConnectionContext> alreadyAuthenChannels = Maps.newConcurrentMap();
    private Map<Channel, byte[]> authChallenges = Maps.newConcurrentMap();
    private final SecureRandom secureRandom = new SecureRandom();

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
        authChallenges.remove(ctx.channel());
        super.channelInactive(ctx);
    }

    private boolean sendAuthencationPackage(Channel channel) {
        byte[] challenge = new byte[20];
        secureRandom.nextBytes(challenge);
        authChallenges.put(channel, challenge);

        ServerGreeting serverGreetingPackage = PackageUtils.buildInitAuthencatinPackage(challenge);
        ByteBuf tmp = PooledByteBufAllocator.DEFAULT.buffer(128);
        try {
            serverGreetingPackage.write(tmp);
            ByteBuf packet = channel.alloc().buffer(tmp.readableBytes() + 4);
            packet.writeMediumLE(tmp.readableBytes());
            packet.writeByte(0x00);
            packet.writeBytes(tmp, tmp.readerIndex(), tmp.readableBytes());
            channel.writeAndFlush(packet);
        } finally {
            ReferenceCountUtil.release(tmp);
        }
        return true;
    }

    public byte[] getAuthChallenge(Channel channel) {
        return authChallenges.get(channel);
    }

    public void clearAuthChallenge(Channel channel) {
        authChallenges.remove(channel);
    }

    public boolean channelHasAuthencation(Channel channel) {
        return alreadyAuthenChannels.containsKey(channel);
    }


}
