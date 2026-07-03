package com.qinyadan.system.dsp.register;

import com.google.common.collect.Sets;
import com.qinyadan.system.dsp.raft.conn.HostAndPort;

import java.util.Set;


public class RegisterAndHeatBeatHandler {
    private Set<HostAndPort> hostSets = Sets.newHashSetWithExpectedSize(10);

    public Set<HostAndPort> getHostSets() {
        return hostSets;
    }

    public void addNodeInfo(HostAndPort ip) {
        hostSets.add(ip);
    }
}
