package com.qinyadan.system.dsp.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Immutable, validated view of DSP runtime configuration.
 *
 * <p>System properties take precedence over environment variables. Loading is
 * intentionally side-effect free so tests and embedded callers can create a
 * fresh snapshot after changing their environment.</p>
 */
public final class DspConfiguration {

    private final int serverPort;
    private final String authUsername;
    private final String authPassword;
    private final Path dataDirectory;
    private final long queryTimeoutMillis;
    private final int queryMaxResultRows;
    private final int queryMaxMaterializedRows;
    private final int writeMaxRowsPerInsert;
    private final int writeMaxRowsPerMutation;
    private final long writeIdempotencyTtlMillis;
    private final int writeIdempotencyMaxKeys;
    private final int storageScanPageSize;
    private final boolean storageSynchronousWrites;
    private final String metadataJdbcUrl;
    private final String metadataJdbcUsername;
    private final String metadataJdbcPassword;

    private DspConfiguration() {
        serverPort = integer("dsp.server.port", "DSP_SERVER_PORT", 3016, 1, 65535);
        authUsername = text("dsp.auth.username", "DSP_AUTH_USERNAME", null);
        authPassword = text("dsp.auth.password", "DSP_AUTH_PASSWORD", null);
        dataDirectory = Paths.get(text("dsp.data.dir", "DSP_DATA_DIR",
                Paths.get(System.getProperty("user.home"), "test", "sloth").toString()));
        queryTimeoutMillis = longValue("dsp.query.timeout-ms", "DSP_QUERY_TIMEOUT_MS",
                30000L, 0L, Long.MAX_VALUE);
        queryMaxResultRows = integer("dsp.query.max-result-rows",
                "DSP_QUERY_MAX_RESULT_ROWS", 100000, 1, Integer.MAX_VALUE);
        queryMaxMaterializedRows = integer("dsp.query.max-materialized-rows",
                "DSP_QUERY_MAX_MATERIALIZED_ROWS", 100000, 1, Integer.MAX_VALUE);
        writeMaxRowsPerInsert = integer("dsp.write.max-rows-per-insert",
                "DSP_WRITE_MAX_ROWS_PER_INSERT", 10000, 1, Integer.MAX_VALUE);
        writeMaxRowsPerMutation = integer("dsp.write.max-rows-per-mutation",
                "DSP_WRITE_MAX_ROWS_PER_MUTATION", 10000, 1, Integer.MAX_VALUE);
        writeIdempotencyTtlMillis = longValue("dsp.write.idempotency-ttl-ms",
                "DSP_WRITE_IDEMPOTENCY_TTL_MS", 300000L, 1000L, Long.MAX_VALUE);
        writeIdempotencyMaxKeys = integer("dsp.write.idempotency-max-keys",
                "DSP_WRITE_IDEMPOTENCY_MAX_KEYS", 10000, 1, Integer.MAX_VALUE);
        storageScanPageSize = integer("dsp.storage.scan-page-size",
                "DSP_STORAGE_SCAN_PAGE_SIZE", 512, 1, 10000);
        storageSynchronousWrites = bool("dsp.storage.sync-writes",
                "DSP_STORAGE_SYNC_WRITES", true);
        metadataJdbcUrl = text("dsp.meta.jdbc.url", "DSP_META_JDBC_URL", null);
        metadataJdbcUsername = text("dsp.meta.jdbc.username",
                "DSP_META_JDBC_USERNAME", null);
        metadataJdbcPassword = text("dsp.meta.jdbc.password",
                "DSP_META_JDBC_PASSWORD", null);
    }

    public static DspConfiguration load() {
        return new DspConfiguration();
    }

    public DspConfiguration validateServer() {
        if (blank(authUsername) || authPassword == null) {
            throw new IllegalStateException(
                    "DSP_AUTH_USERNAME and DSP_AUTH_PASSWORD are required");
        }
        if (!blank(metadataJdbcUrl) && blank(metadataJdbcUsername)) {
            throw new IllegalStateException(
                    "DSP_META_JDBC_USERNAME is required when DSP_META_JDBC_URL is set");
        }
        return this;
    }

    public int getServerPort() { return serverPort; }
    public String getAuthUsername() { return authUsername; }
    public String getAuthPassword() { return authPassword; }
    public Path getDataDirectory() { return dataDirectory; }
    public long getQueryTimeoutMillis() { return queryTimeoutMillis; }
    public int getQueryMaxResultRows() { return queryMaxResultRows; }
    public int getQueryMaxMaterializedRows() { return queryMaxMaterializedRows; }
    public int getWriteMaxRowsPerInsert() { return writeMaxRowsPerInsert; }
    public int getWriteMaxRowsPerMutation() { return writeMaxRowsPerMutation; }
    public long getWriteIdempotencyTtlMillis() { return writeIdempotencyTtlMillis; }
    public int getWriteIdempotencyMaxKeys() { return writeIdempotencyMaxKeys; }
    public int getStorageScanPageSize() { return storageScanPageSize; }
    public boolean isStorageSynchronousWrites() { return storageSynchronousWrites; }
    public String getMetadataJdbcUrl() { return metadataJdbcUrl; }
    public String getMetadataJdbcUsername() { return metadataJdbcUsername; }
    public String getMetadataJdbcPassword() { return metadataJdbcPassword; }

    private static String text(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(environment);
        }
        return blank(value) ? defaultValue : value.trim();
    }

    private static int integer(String property, String environment, int defaultValue,
                               int minimum, int maximum) {
        String value = text(property, environment, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException(property + " must be between "
                        + minimum + " and " + maximum + ": " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + property + ": " + value, e);
        }
    }

    private static long longValue(String property, String environment, long defaultValue,
                                  long minimum, long maximum) {
        String value = text(property, environment, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException(property + " must be between "
                        + minimum + " and " + maximum + ": " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long for " + property + ": " + value, e);
        }
    }

    private static boolean bool(String property, String environment, boolean defaultValue) {
        String value = text(property, environment, null);
        if (value == null) {
            return defaultValue;
        }
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(
                    property + " must be true or false: " + value);
        }
        return Boolean.parseBoolean(value);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
