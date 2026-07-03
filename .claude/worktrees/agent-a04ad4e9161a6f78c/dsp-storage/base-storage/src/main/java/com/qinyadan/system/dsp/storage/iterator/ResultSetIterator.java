package com.qinyadan.system.dsp.storage.iterator;


public interface ResultSetIterator<T> {

    boolean hasNext();


    T next();
}
