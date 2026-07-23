package com.qinyadan.system.dsp.schema.common.enumerator;

import org.apache.calcite.linq4j.Enumerator;

import java.util.Iterator;


public class BasicEnumerator implements Enumerator<Object[]> {

    private final Iterator<Object[]> iterator;
    private final AutoCloseable closeable;
    private boolean closed;

    public BasicEnumerator(Iterator<Object[]> iterator) {
        this(iterator, null);
    }

    public BasicEnumerator(Iterator<Object[]> iterator, AutoCloseable closeable) {
        if (iterator == null) {
            throw new IllegalArgumentException("Iterator must not be null");
        }
        this.iterator = iterator;
        this.closeable = closeable;
    }

    private Object[] current;

    @Override
    public Object[] current() {
        return current;
    }

    @Override
    public boolean moveNext() {
        if (closed) {
            return false;
        }
        if (iterator.hasNext()) {
            current = iterator.next();
            return true;
        } else {
            current = null;
            return false;
        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Schema enumerators cannot be reset");
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        current = null;
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to close schema enumerator", e);
            }
        }
    }
}
