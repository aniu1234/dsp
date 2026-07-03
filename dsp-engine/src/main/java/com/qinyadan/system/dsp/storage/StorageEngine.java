package com.qinyadan.system.dsp.storage;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.data.value.Value;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


public interface StorageEngine extends LifeCycle {

    /**
     * Insert row
     *
     * @param row
     * @return
     */
    boolean insert(List<List<Value>> row) throws IOException;

    /**
     * Query
     *
     * @param queryContext
     */
    Iterator<SlothRow> query(QueryContext queryContext) throws IOException;


    /**
     * Mark read only
     *
     * @return
     */
    default boolean readOnly() {
        return false;
    }


    /**
     * @param readOnly
     */
    default void setReadOnly(boolean readOnly) {
    }

    /**
     * flush data in memory to disk and update index
     */
    default void flush() {
    }


    default boolean shouldFlush() {
        return false;
    }
}
