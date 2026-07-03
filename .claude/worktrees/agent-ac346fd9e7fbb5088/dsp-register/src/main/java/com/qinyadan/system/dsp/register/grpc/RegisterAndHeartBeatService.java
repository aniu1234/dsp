package com.qinyadan.system.dsp.register.grpc;

import com.qinyadan.system.dsp.raft.conn.HostAndPort;
import com.qinyadan.system.dsp.raft.generated.RegisterAndHeartBeatProtos;
import com.qinyadan.system.dsp.raft.generated.RegisterAndHeartBeatServiceGrpc;
import com.qinyadan.system.dsp.register.RegisterAndHeatBeatHandler;
import io.grpc.stub.StreamObserver;


public class RegisterAndHeartBeatService extends RegisterAndHeartBeatServiceGrpc.RegisterAndHeartBeatServiceImplBase {
    private RegisterAndHeatBeatHandler registerAndHeatBeatHandler;

    public RegisterAndHeartBeatService(RegisterAndHeatBeatHandler registerAndHeatBeatHandler) {
        this.registerAndHeatBeatHandler = registerAndHeatBeatHandler;
    }

    @Override
    public void registerNodeInfo(RegisterAndHeartBeatProtos.NodeRegisterRequest request,
                                 StreamObserver<RegisterAndHeartBeatProtos.NodeRegisterReponse> responseObserver) {
        RegisterAndHeartBeatProtos.NodeRegisterReponse r =
                RegisterAndHeartBeatProtos.NodeRegisterReponse.newBuilder().setCode(0).build();
        final String host = request.getHostname();
        final int port = request.getPort();

        registerAndHeatBeatHandler.addNodeInfo(new HostAndPort(host, port));
        responseObserver.onNext(r);
        responseObserver.onCompleted();
    }
}
