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
