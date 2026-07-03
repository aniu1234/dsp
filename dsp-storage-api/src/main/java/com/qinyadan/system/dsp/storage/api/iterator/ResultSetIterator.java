package com.qinyadan.system.dsp.storage.api.iterator;

/**
 * Result set iterator interface for storage engines.
 */
public interface ResultSetIterator<T> {

    boolean hasNext();

    T next();
}
