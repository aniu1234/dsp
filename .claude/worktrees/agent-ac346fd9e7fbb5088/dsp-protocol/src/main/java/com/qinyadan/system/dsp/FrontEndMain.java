package com.qinyadan.system.dsp;

import com.google.common.base.Throwables;
import com.qinyadan.system.dsp.protocol.config.ConnectionConfig;
import com.qinyadan.system.dsp.protocol.pkg.netty.AuthenticationHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.ByteBufToPackageDecoder;
import com.qinyadan.system.dsp.protocol.pkg.netty.MysqlPackageHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.NettyConnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import static com.qinyadan.system.dsp.constant.Constants.CPU_CORES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;


@Slf4j
public class FrontEndMain {

    public static void main(String[] args) {

        int port = 3016;

        LifeCycleInstance.start();

        //start netty port
        try {
            final EventLoopGroup boss = new NioEventLoopGroup(1);
            final EventLoopGroup work = new NioEventLoopGroup(CPU_CORES * 2);
            final ServerBootstrap serverBootstrap = new ServerBootstrap();

            serverBootstrap
                    .group(boss, work)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 8196)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    final ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("open_channler", new NettyConnectionHandler());

                    pipeline.addLast("read_timeout_handler", new ReadTimeoutHandler(ConnectionConfig.READ_TIMEOUT));
                    pipeline.addLast("write_time_handler", new WriteTimeoutHandler(ConnectionConfig.WRITE_TIMEOUT));

                    pipeline.addLast("bytebuf_to_bytebuf_decoder", new LengthFieldBasedFrameDecoder(
                            LITTLE_ENDIAN,
                            Integer.MAX_VALUE,
                            0,
                            3,
                            1,
                            0,
                            true
                    ));

                    pipeline.addLast("bytebuf_to_buffer_decoder", new ByteBufToPackageDecoder());
                    pipeline.addLast("authentication", new AuthenticationHandler());
                    pipeline.addLast("query_handler", new MysqlPackageHandler());
                }
            });

            serverBootstrap.validate();
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            channelFuture.channel().closeFuture().addListener((GenericFutureListener) future -> {
                log.info("channelFuture listener{}", future.toString());
            });
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        }
    }
}
