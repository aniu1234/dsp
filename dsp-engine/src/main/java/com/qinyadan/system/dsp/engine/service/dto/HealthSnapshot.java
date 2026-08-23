package com.qinyadan.system.dsp.engine.service.dto;

public final class HealthSnapshot {

    private final String status;
    private final String detail;
    private final int databases;
    private final int tables;
    private final int storageEngines;
    private final int readOnlyStorageEngines;
    private final long uptimeMillis;

    public HealthSnapshot(String status, String detail, int databases, int tables,
                          int storageEngines, int readOnlyStorageEngines,
                          long uptimeMillis) {
        this.status = status;
        this.detail = detail;
        this.databases = databases;
        this.tables = tables;
        this.storageEngines = storageEngines;
        this.readOnlyStorageEngines = readOnlyStorageEngines;
        this.uptimeMillis = uptimeMillis;
    }

    public String getStatus() { return status; }
    public String getDetail() { return detail; }
    public int getDatabases() { return databases; }
    public int getTables() { return tables; }
    public int getStorageEngines() { return storageEngines; }
    public int getReadOnlyStorageEngines() { return readOnlyStorageEngines; }
    public long getUptimeMillis() { return uptimeMillis; }
}
