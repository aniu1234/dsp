package com.qinyadan.system.dsp.raft;


public class StorageServerMain {

    public static void main(String[] args) {
        int storageServerPort = 4100;
        String executionServerHost = "localhost";
        int executionServerPort = 3100;

        StorageServer storageServer = new StorageServer(storageServerPort, executionServerHost, executionServerPort);
        storageServer.start();
    }
}
