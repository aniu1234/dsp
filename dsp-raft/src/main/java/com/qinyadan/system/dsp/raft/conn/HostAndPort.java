package com.qinyadan.system.dsp.raft.conn;

import java.util.Objects;


public class HostAndPort {
    private final String ip;
    private final int port;

    public HostAndPort(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostAndPort ipAndPort = (HostAndPort) o;
        return Objects.equals(ip, ipAndPort.ip)
                && Objects.equals(port, ipAndPort.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
