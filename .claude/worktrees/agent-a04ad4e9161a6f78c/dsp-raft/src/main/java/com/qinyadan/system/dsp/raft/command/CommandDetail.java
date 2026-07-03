package com.qinyadan.system.dsp.raft.command;


public class CommandDetail {
    private final String key;
    private final String value;

    private final String table;

    public CommandDetail(String key, String value, String table) {
        this.key = key;
        this.value = value;
        this.table = table;
    }
}
