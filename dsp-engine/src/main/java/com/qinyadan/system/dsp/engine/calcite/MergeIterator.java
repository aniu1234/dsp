package com.qinyadan.system.dsp.engine.calcite;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class MergeIterator<E> implements Iterator<E> {

    private List<Iterator<E>> allValues;
    private int index;
    private int valueSize;

    public MergeIterator(List<Iterator<E>> allValues) {
        this.allValues = allValues;
        index = 0;
        valueSize = allValues.size();
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
