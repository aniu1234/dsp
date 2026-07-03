package com.qinyadan.system.dsp.raft.grpc;

import com.qinyadan.system.dsp.raft.generated.DdlProtos;
import com.qinyadan.system.dsp.raft.generated.DdlServiceGrpc;
import io.grpc.stub.StreamObserver;


public class DdlService extends DdlServiceGrpc.DdlServiceImplBase {


    @Override
    public void createTable(DdlProtos.CreateTableRequest request, StreamObserver<DdlProtos.CreateTableResponse> responseObserver) {
        super.createTable(request, responseObserver);
    }
}
