package com.qinyadan.system.dsp.register;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.raft.Startable;
import com.qinyadan.system.dsp.raft.conn.HostAndPort;
import com.qinyadan.system.dsp.raft.grpc.GrpcServer;
import com.qinyadan.system.dsp.register.grpc.DdlServiceClient;
import com.qinyadan.system.dsp.register.grpc.RegisterAndHeartBeatService;
import io.grpc.BindableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ConfigurationCenter extends GrpcServer implements Startable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationCenter.class);
    private static final Map<HostAndPort, DdlServiceClient> RPC_CLIENT_MAP = Maps.newHashMap();
    private RegisterAndHeatBeatHandler registerAndHeatBeatHandler = new RegisterAndHeatBeatHandler();

    public ConfigurationCenter(int port) {
        super(port);
    }

    @Override
    public List<BindableService> getService() {
        return Collections.singletonList(new RegisterAndHeartBeatService(registerAndHeatBeatHandler));
    }

    @Override
    public void start() {
        //start rpc
        startRpc();

        //block
        block();
    }
}
