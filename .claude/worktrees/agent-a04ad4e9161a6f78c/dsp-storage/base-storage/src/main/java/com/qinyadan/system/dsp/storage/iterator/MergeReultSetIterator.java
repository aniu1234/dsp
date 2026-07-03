package com.qinyadan.system.dsp.storage.iterator;


public class MergeReultSetIterator<T> implements ResultSetIterator<T> {
    //merge realtime engine and block reader


    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        return null;
    }
}
