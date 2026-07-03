package com.qinyadan.system.dsp.storage.parser.util;

import java.util.Iterator;
import java.util.List;


public class SlothMergeIterator<E> implements Iterator<E> {

    private List<Iterator<E>> allValues;
    private int index;
    private E current;

    private int valueSize;

    public SlothMergeIterator(List<Iterator<E>> allValues) {
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
