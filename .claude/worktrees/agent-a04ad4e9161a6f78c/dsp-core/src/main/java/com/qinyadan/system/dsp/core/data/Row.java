package com.qinyadan.system.dsp.core.data;

import com.qinyadan.system.dsp.core.data.value.Value;

public interface Row {
    int columnSize();
    Value getColumn(int i);
    Value[] getColumns();
}
