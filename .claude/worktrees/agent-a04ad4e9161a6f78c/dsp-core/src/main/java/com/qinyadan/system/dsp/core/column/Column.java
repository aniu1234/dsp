package com.qinyadan.system.dsp.core.column;

public interface Column {
    int size();
    Object get(int index);
    boolean isNull(int index);
    void append(Object value);
    void flush();
}
