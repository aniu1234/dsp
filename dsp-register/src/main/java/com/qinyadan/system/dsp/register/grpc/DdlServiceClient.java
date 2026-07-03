package com.qinyadan.system.dsp.register.grpc;

import com.qinyadan.system.dsp.raft.generated.DdlProtos;
import com.qinyadan.system.dsp.raft.generated.DdlServiceGrpc;
import com.qinyadan.system.dsp.raft.grpc.GrpcClient;


public class DdlServiceClient extends GrpcClient {

    private DdlServiceGrpc.DdlServiceBlockingStub stub;

    public DdlServiceClient(String host, int port) {
        super(host, port);
    }

    private void executeCreateTable(String tableName) {
        if (stub == null) {
            stub = DdlServiceGrpc.newBlockingStub(managedChannel);
        }

        DdlProtos.CreateTableRequest.Builder b = DdlProtos.CreateTableRequest.newBuilder();
        b.setTableName(tableName);
        DdlProtos.CreateTableResponse r = stub.createTable(b.build());
    }

}
