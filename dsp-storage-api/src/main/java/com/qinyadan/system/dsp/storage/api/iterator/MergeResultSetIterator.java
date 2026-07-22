package com.qinyadan.system.dsp.storage.api.iterator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Merges multiple iterators into a single sequential iterator.
 * Iterators are consumed in order; exhausted iterators are skipped.
 */
public class MergeResultSetIterator<E> implements ResultSetIterator<E> {

    private final List<Iterator<E>> allValues;
    private int index;
    private final int valueSize;

    public MergeResultSetIterator(List<Iterator<E>> allValues) {
        this.allValues = allValues;
        this.index = 0;
        this.valueSize = allValues.size();
    }

    @Override
    public boolean hasNext() {
        while (index < valueSize) {
            Iterator<E> it = allValues.get(index);
            if (it.hasNext()) {
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return allValues.get(index).next();
    }
}
