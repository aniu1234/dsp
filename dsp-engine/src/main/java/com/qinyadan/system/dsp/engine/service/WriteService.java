package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.engine.service.dto.WriteColumn;
import com.qinyadan.system.dsp.engine.service.dto.WriteOutcome;
import com.qinyadan.system.dsp.engine.service.dto.WriteRequest;
import com.qinyadan.system.dsp.engine.service.dto.WriteTable;
import com.qinyadan.system.dsp.storage.api.WriteResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Application boundary for validated statement-level table writes.
 */
public final class WriteService {

    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    public static final WriteService INSTANCE = new WriteService();

    private final Map<String, IdempotencyEntry> idempotencyEntries =
            new ConcurrentHashMap<>();
    private final Object[] idempotencyLocks = new Object[64];
    private final Object idempotencyCapacityLock = new Object();
    private int pendingIdempotencyEntries;

    private WriteService() {
        for (int i = 0; i < idempotencyLocks.length; i++) {
            idempotencyLocks[i] = new Object();
        }
    }

    public WriteTable describe(String database, String tableName) {
        SlothTable table = CatalogService.INSTANCE.getTable(database, tableName);
        if (table == null || table.getSlothTableEngine() == null) {
            return null;
        }
        return describe(database, tableName, table);
    }

    private WriteTable describe(String database, String tableName, SlothTable table) {
        SlothTableEngine engine = table.getSlothTableEngine();
        List<WriteColumn> columns = new ArrayList<>(table.getColumns().size());
        for (SlothColumn column : table.getColumns()) {
            EnhanceSlothColumn definition = column.getColumnType();
            columns.add(new WriteColumn(column.getColumnName(),
                    engine.getColumnAndDataType().get(column.getColumnName()),
                    definition.isNullable(), definition.isUnsigned(),
                    definition.getPrecision(), definition.getDefalutValue()));
        }
        return new WriteTable(database, tableName, columns);
    }

    public WriteResult insert(String database, String tableName,
                              List<List<Value>> rows) {
        return insert(new WriteRequest(database, tableName, rows, null)).getResult();
    }

    public WriteOutcome insert(WriteRequest request) {
        try {
            WriteOutcome outcome = doInsert(request);
            if (outcome.isIdempotentReplay()) {
                RuntimeMetrics.INSTANCE.idempotentReplay();
            } else {
                RuntimeMetrics.INSTANCE.writeSucceeded(outcome.getResult().getInserted());
            }
            return outcome;
        } catch (RuntimeException e) {
            RuntimeMetrics.INSTANCE.writeFailed();
            throw e;
        }
    }

    private WriteOutcome doInsert(WriteRequest request) {
        if (request == null || blank(request.getDatabase()) || blank(request.getTable())) {
            throw failure(WriteErrorCode.INVALID_REQUEST,
                    "Write database and table are required");
        }
        String database = request.getDatabase();
        String tableName = request.getTable();
        SlothTable slothTable = CatalogService.INSTANCE.getTable(database, tableName);
        if (slothTable == null || slothTable.getSlothTableEngine() == null) {
            throw failure(WriteErrorCode.UNKNOWN_TABLE,
                    "Unknown table: " + database + "." + tableName);
        }
        validate(describe(database, tableName, slothTable), request.getRows());
        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        if (idempotencyKey == null) {
            return new WriteOutcome(append(slothTable, request.getRows()), false);
        }
        cleanupIdempotencyEntries();
        String cacheKey = database + "." + tableName + ":" + idempotencyKey;
        String fingerprint = fingerprint(request.getRows());
        Object lock = idempotencyLocks[(cacheKey.hashCode() & Integer.MAX_VALUE)
                % idempotencyLocks.length];
        synchronized (lock) {
            IdempotencyEntry existing = idempotencyEntries.get(cacheKey);
            long now = System.currentTimeMillis();
            if (existing != null && !existing.expired(now)) {
                if (!existing.fingerprint.equals(fingerprint)) {
                    throw failure(WriteErrorCode.IDEMPOTENCY_CONFLICT,
                            "Idempotency key was already used with a different payload");
                }
                return new WriteOutcome(existing.result, true);
            }
            if (existing != null) {
                idempotencyEntries.remove(cacheKey, existing);
            }
            DspConfiguration configuration = DspConfiguration.load();
            reserveIdempotencyCapacity(configuration.getWriteIdempotencyMaxKeys());
            boolean reservationHeld = true;
            try {
                WriteResult result = append(slothTable, request.getRows());
                long completedAt = System.currentTimeMillis();
                long ttl = configuration.getWriteIdempotencyTtlMillis();
                long expiresAt = ttl > Long.MAX_VALUE - completedAt
                        ? Long.MAX_VALUE : completedAt + ttl;
                storeIdempotencyEntry(cacheKey,
                        new IdempotencyEntry(fingerprint, result, expiresAt));
                reservationHeld = false;
                return new WriteOutcome(result, false);
            } finally {
                if (reservationHeld) {
                    releaseIdempotencyCapacity();
                }
            }
        }
    }

    public int maximumRowsPerInsert() {
        return DspConfiguration.load().getWriteMaxRowsPerInsert();
    }

    private void validate(WriteTable table, List<List<Value>> rows) {
        if (rows == null) {
            throw failure(WriteErrorCode.INVALID_REQUEST,
                    "Rows to insert must not be null");
        }
        if (rows.isEmpty()) {
            throw failure(WriteErrorCode.INVALID_REQUEST,
                    "Rows to insert must not be empty");
        }
        if (rows.size() > maximumRowsPerInsert()) {
            throw failure(WriteErrorCode.RESOURCE_LIMIT,
                    "Write exceeds the configured row limit");
        }
        List<WriteColumn> columns = table.getColumns();
        for (List<Value> row : rows) {
            if (row == null || row.size() != columns.size()) {
                throw failure(WriteErrorCode.INVALID_REQUEST,
                        "Write row does not match the table column count");
            }
            for (int i = 0; i < row.size(); i++) {
                validateValue(columns.get(i), row.get(i));
            }
        }
    }

    private void validateValue(WriteColumn column, Value value) {
        if (value == null) {
            throw failure(WriteErrorCode.INVALID_REQUEST,
                    "Write values must use typed Value objects");
        }
        if (value.getType() == null) {
            throw failure(WriteErrorCode.TYPE_MISMATCH,
                    "Value type is required for column '" + column.getName() + "'");
        }
        if (value.isNull()) {
            if (!column.isNullable()) {
                throw failure(WriteErrorCode.CONSTRAINT_VIOLATION,
                        "Column '" + column.getName() + "' cannot be null");
            }
            return;
        }
        if (!column.getDataType().equals(value.getType())) {
            throw failure(WriteErrorCode.TYPE_MISMATCH,
                    "Value type does not match column '" + column.getName() + "'");
        }
        Object raw = value.getValueByType();
        if (column.isUnsigned() && raw instanceof Number
                && new BigDecimal(raw.toString()).signum() < 0) {
            throw failure(WriteErrorCode.CONSTRAINT_VIOLATION,
                    "Unsigned column cannot contain a negative value");
        }
        if (column.getPrecision() > 0 && raw instanceof String) {
            String string = (String) raw;
            if (string.codePointCount(0, string.length()) > column.getPrecision()) {
                throw failure(WriteErrorCode.CONSTRAINT_VIOLATION,
                        "Value exceeds declared column precision");
            }
        }
    }

    private WriteResult append(SlothTable table, List<List<Value>> rows) {
        try {
            return table.getSlothTableEngine().insert(rows);
        } catch (WriteServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new WriteServiceException(WriteErrorCode.STORAGE_FAILURE,
                    "Storage write failed", e);
        }
    }

    private String normalizeIdempotencyKey(String key) {
        if (blank(key)) {
            return null;
        }
        String normalized = key.trim();
        if (!IDEMPOTENCY_KEY.matcher(normalized).matches()) {
            throw failure(WriteErrorCode.INVALID_REQUEST,
                    "Idempotency key must contain 1-128 safe characters");
        }
        return normalized;
    }

    private String fingerprint(List<List<Value>> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateInteger(digest, rows.size());
            for (List<Value> row : rows) {
                updateInteger(digest, row.size());
                for (Value value : row) {
                    updateInteger(digest, value.getType().id());
                    Object raw = value.getValueByType();
                    if (raw == null) {
                        updateInteger(digest, -1);
                    } else {
                        updateString(digest, raw.toString());
                    }
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void updateString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInteger(digest, bytes.length);
        digest.update(bytes);
    }

    private void updateInteger(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private void cleanupIdempotencyEntries() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, IdempotencyEntry> entry : idempotencyEntries.entrySet()) {
            if (entry.getValue().expired(now)) {
                idempotencyEntries.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private void reserveIdempotencyCapacity(int maximum) {
        synchronized (idempotencyCapacityLock) {
            if (idempotencyEntries.size() + pendingIdempotencyEntries >= maximum) {
                throw failure(WriteErrorCode.RESOURCE_LIMIT,
                        "Idempotency cache reached its configured key limit");
            }
            pendingIdempotencyEntries++;
        }
    }

    private void storeIdempotencyEntry(String key, IdempotencyEntry entry) {
        synchronized (idempotencyCapacityLock) {
            idempotencyEntries.put(key, entry);
            pendingIdempotencyEntries--;
        }
    }

    private void releaseIdempotencyCapacity() {
        synchronized (idempotencyCapacityLock) {
            pendingIdempotencyEntries--;
        }
    }

    private WriteServiceException failure(WriteErrorCode code, String message) {
        return new WriteServiceException(code, message);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class IdempotencyEntry {
        private final String fingerprint;
        private final WriteResult result;
        private final long expiresAt;

        private IdempotencyEntry(String fingerprint, WriteResult result, long expiresAt) {
            this.fingerprint = fingerprint;
            this.result = result;
            this.expiresAt = expiresAt;
        }

        private boolean expired(long now) {
            return now >= expiresAt;
        }
    }
}
