package com.qinyadan.system.dsp.engine.data;

import com.qinyadan.system.dsp.engine.data.value.Value;

import java.util.List;


public class SlothRow implements Row<Value> {

    public static final SlothRow EOF_ROW = new SlothRow();

    private List<Value> rowValue;

    public SlothRow() {
    }

    public SlothRow(List<Value> rowValue) {
        this.rowValue = rowValue;
    }


    public void setRowValue(List<Value> rowValue) {
        this.rowValue = rowValue;
    }

    @Override
    public int columnSize() {
        return rowValue.size();
    }

    @Override
    public Value getColumn(int i) {
        return rowValue.get(i);
    }

    @Override
    public List<Value> getAllColumn() {
        return rowValue;
    }
}
