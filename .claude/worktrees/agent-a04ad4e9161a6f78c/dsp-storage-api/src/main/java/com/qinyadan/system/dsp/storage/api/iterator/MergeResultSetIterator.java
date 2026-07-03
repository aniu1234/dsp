package com.qinyadan.system.dsp.storage.api.iterator;

import java.util.Iterator;
import java.util.List;

/**
 * Merges multiple iterators into a single sequential iterator.
 * Iterators are consumed in order; exhausted iterators are skipped.
 */
public class MergeResultSetIterator<E> implements ResultSetIterator<E> {

    private final List<Iterator<E>> allValues;
    private int index;
    private E current;
    private int valueSize;

    public MergeResultSetIterator(List<Iterator<E>> allValues) {
        this.allValues = allValues;
        this.index = 0;
        this.current = null;
        this.valueSize = allValues.size();
    }

    @Override
    public boolean hasNext() {
        while (index < valueSize) {
            Iterator<E> it = allValues.get(index);
            if (it.hasNext()) {
                current = it.next();
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public E next() {
        return current;
    }
}
