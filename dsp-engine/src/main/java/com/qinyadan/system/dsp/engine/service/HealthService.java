package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.service.dto.HealthSnapshot;
import com.qinyadan.system.dsp.engine.service.dto.MetricsSnapshot;
import com.qinyadan.system.dsp.storage.api.StorageEngine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read-only process health inspection.
 */
public final class HealthService {

    public static final HealthService INSTANCE = new HealthService();

    private HealthService() {
    }

    public HealthSnapshot snapshot() {
        try {
            DspConfiguration configuration = DspConfiguration.load();
            int databases = CatalogService.INSTANCE.listDatabases().size();
            int tables = 0;
            int engines = 0;
            int readOnly = 0;
            for (String database : CatalogService.INSTANCE.listDatabases()) {
                for (String tableName : CatalogService.INSTANCE.listTables(database)) {
                    tables++;
                    SlothTable table = CatalogService.INSTANCE.getTable(database, tableName);
                    if (table == null || table.getSlothTableEngine() == null) {
                        continue;
                    }
                    for (StorageEngine engine : table.getSlothTableEngine().getStorageEngines()) {
                        engines++;
                        if (engine.readOnly()) {
                            readOnly++;
                        }
                    }
                }
            }
            Path dataDirectory = configuration.getDataDirectory();
            boolean writable = writableDirectory(dataDirectory);
            String status = readOnly == 0 && writable ? "UP" : "DEGRADED";
            String detail = !writable ? "data directory is not writable"
                    : readOnly > 0 ? readOnly + " storage engine(s) are read only" : "ready";
            MetricsSnapshot metrics = RuntimeMetrics.INSTANCE.snapshot();
            return new HealthSnapshot(status, detail, databases, tables, engines,
                    readOnly, metrics.getUptimeMillis());
        } catch (RuntimeException e) {
            return new HealthSnapshot("DOWN", e.getMessage(), 0, 0, 0, 0,
                    RuntimeMetrics.INSTANCE.snapshot().getUptimeMillis());
        }
    }

    private boolean writableDirectory(Path directory) {
        Path candidate = directory.toAbsolutePath();
        if (Files.exists(candidate)) {
            return Files.isDirectory(candidate) && Files.isWritable(candidate);
        }
        candidate = candidate.getParent();
        while (candidate != null && !Files.exists(candidate)) {
            candidate = candidate.getParent();
        }
        return candidate != null && Files.isDirectory(candidate)
                && Files.isWritable(candidate);
    }
}
