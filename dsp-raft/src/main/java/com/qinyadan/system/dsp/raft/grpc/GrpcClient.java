package com.qinyadan.system.dsp.raft.grpc;

import com.qinyadan.system.dsp.raft.Startable;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;


public class GrpcClient implements AutoCloseable, Startable {
    public static final Logger LOGGER = LoggerFactory.getLogger(GrpcClient.class);

    protected String host;
    protected int port;
    protected boolean useSSL = false;

    protected ManagedChannel managedChannel;

    public GrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    @Override
    public void start() {
        if (useSSL) {
            try {
                managedChannel = NettyChannelBuilder
                        .forAddress(host, port)
                        .sslContext(GrpcSslContexts.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build()).build();
            } catch (SSLException e) {
                LOGGER.error("Start client:", e);
                throw new RuntimeException(e);
            }
        } else {
            managedChannel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
        }
    }

    @Override
    public void close() throws Exception {
        if (managedChannel != null) {
            managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
