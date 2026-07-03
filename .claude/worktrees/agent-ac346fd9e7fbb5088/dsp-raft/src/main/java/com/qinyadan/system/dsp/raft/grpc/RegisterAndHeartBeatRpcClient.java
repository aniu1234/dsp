package com.qinyadan.system.dsp.raft.grpc;

import com.qinyadan.system.dsp.raft.generated.RegisterAndHeartBeatProtos;
import com.qinyadan.system.dsp.raft.generated.RegisterAndHeartBeatServiceGrpc;


public class RegisterAndHeartBeatRpcClient extends GrpcClient {
    private RegisterAndHeartBeatServiceGrpc.RegisterAndHeartBeatServiceBlockingStub stub;

    public RegisterAndHeartBeatRpcClient(String host, int port) {
        super(host, port);
    }

    public void registerLocation(String host, int port) {
        RegisterAndHeartBeatProtos.NodeRegisterRequest.Builder builder
                = RegisterAndHeartBeatProtos.NodeRegisterRequest.newBuilder();

        builder.setHostname(host);
        builder.setPort(port);

        if (stub == null) {
            stub = RegisterAndHeartBeatServiceGrpc.newBlockingStub(managedChannel);
        }

        RegisterAndHeartBeatProtos.NodeRegisterReponse r = stub.registerNodeInfo(builder.build());

        if (r.getCode() == 0) {
            LOGGER.info("Register stoarge successful to execution server...");
        } else {
            LOGGER.error("Failed register...");
        }
    }
}
