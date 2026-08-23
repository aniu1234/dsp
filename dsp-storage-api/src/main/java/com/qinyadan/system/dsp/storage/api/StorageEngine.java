package com.qinyadan.system.dsp.storage.api;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public interface StorageEngine {

    Logger LOG = LoggerFactory.getLogger(StorageEngine.class);

    /**
     * Append rows to this engine as one batch.
     *
     * <p>Implementations must validate the complete batch before mutating storage and
     * return the exact number of appended rows. A successful append makes rows available
     * to scans; durability is controlled separately through {@link #flush()}.</p>
     */
    WriteResult append(Row... rows) throws IOException;

    /**
     * Replace the complete visible row set as one storage-level mutation.
     *
     * <p>Implementations that support mutations must validate the replacement batch
     * before changing visible state. The default keeps append-only engines compatible
     * while making unsupported mutation explicit.</p>
     */
    default WriteResult replaceAll(Row... rows) throws IOException {
        throw new UnsupportedOperationException(
                "Full-table replacement is not supported by " + getClass().getSimpleName());
    }

    /**
     * Scan rows matching the query.
     */
    Iterator<Row> scan(QueryContext queryContext) throws IOException;

    /**
     * Estimate row count.
     */
    long estimateRowCount();

    /**
     * Flush in-memory data to persistent storage.
     */
    default void flush() {
        LOG.info("flush not implemented for {}", getClass().getSimpleName());
    }

    /**
     * Check if flush is needed.
     */
    default boolean shouldFlush() {
        return false;
    }

    /**
     * Close resources.
     */
    void close();

    /**
     * Check if this engine is read-only.
     */
    default boolean readOnly() { return false; }

    /**
     * Set read-only mode.
     */
    default void setReadOnly(boolean readOnly) {}
}
