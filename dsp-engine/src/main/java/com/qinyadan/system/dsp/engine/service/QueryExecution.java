package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Protocol-neutral cursor over an engine query.
 */
public final class QueryExecution implements AutoCloseable {

    private final List<QueryColumn> columns;
    private final Operator<SlothRow> operator;
    private boolean openAttempted;
    private boolean closed;

    QueryExecution(List<QueryColumn> columns, Operator<SlothRow> operator) {
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.operator = operator;
    }

    public List<QueryColumn> getColumns() {
        return columns;
    }

    public void open() {
        if (openAttempted) {
            throw new IllegalStateException("Query execution has already been opened");
        }
        openAttempted = true;
        operator.open();
    }

    /**
     * Return the next converted row, or {@code null} after the end of the result.
     */
    public List<Object> nextRow() {
        if (!openAttempted || closed) {
            throw new IllegalStateException("Query execution is not open");
        }
        SlothRow row = operator.next();
        if (row == SlothRow.EOF_ROW) {
            return null;
        }
        List<Object> values = new ArrayList<>(row.columnSize());
        for (Value value : row.getAllColumn()) {
            values.add(value == null ? null : value.getValueByType());
        }
        return values;
    }

    @Override
    public void close() {
        if (!openAttempted || closed) {
            return;
        }
        closed = true;
        operator.close();
    }
}
