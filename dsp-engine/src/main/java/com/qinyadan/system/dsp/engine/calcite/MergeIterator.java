package com.qinyadan.system.dsp.engine.calcite;

import java.util.Iterator;
import java.util.List;


public class MergeIterator<E> implements Iterator<E> {

    private List<Iterator<E>> allValues;
    private int index;
    private E current;

    private int valueSize;

    public MergeIterator(List<Iterator<E>> allValues) {
        this.allValues = allValues;
        index = 0;
        current = null;

        valueSize = allValues.size();
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
