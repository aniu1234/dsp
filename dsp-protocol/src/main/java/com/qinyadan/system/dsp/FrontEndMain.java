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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.qinyadan.system.dsp.constant.Constants.CPU_CORES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;


@Slf4j
public class FrontEndMain {

    public static void main(String[] args) {

        int port = getServerPort();

        LifeCycleInstance.start();
        final EventLoopGroup boss = new NioEventLoopGroup(1);
        final EventLoopGroup work = new NioEventLoopGroup(CPU_CORES * 2);
        final EventExecutorGroup queryExecutors = new DefaultEventExecutorGroup(CPU_CORES);
        final AtomicReference<Channel> serverChannel = new AtomicReference<>();
        final Thread shutdownHook = new Thread(() -> {
            Channel channel = serverChannel.get();
            if (channel != null) {
                channel.close().syncUninterruptibly();
            }
        }, "dsp-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        //start netty port
        try {
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
                    pipeline.addLast("open_channel", NettyConnectionHandler.INSTANCE);

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
                    pipeline.addLast(queryExecutors, "query_handler", new MysqlPackageHandler());
                }
            });

            serverBootstrap.validate();
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            serverChannel.set(channelFuture.channel());
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Frontend server interrupted", e);
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // The JVM is already running shutdown hooks.
            }
            queryExecutors.shutdownGracefully().syncUninterruptibly();
            work.shutdownGracefully().syncUninterruptibly();
            boss.shutdownGracefully().syncUninterruptibly();
            LifeCycleInstance.closeAll();
        }
    }

    private static int getServerPort() {
        String configured = System.getProperty("dsp.server.port");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("DSP_SERVER_PORT");
        }
        if (configured == null || configured.trim().isEmpty()) {
            return 3016;
        }
        try {
            int port = Integer.parseInt(configured.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535: " + port);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid DSP server port: " + configured, e);
        }
    }
}
