package com.qinyadan.system.dsp.engine.data;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class SlothRow implements Row {

    public static final SlothRow EOF_ROW = new SlothRow(Collections.emptyList());

    private final List<Value> rowValue;

    public SlothRow(List<Value> rowValue) {
        this.rowValue = Collections.unmodifiableList(new ArrayList<>(rowValue));
    }

    @Override
    public int columnSize() {
        return rowValue.size();
    }

    @Override
    public Value getColumn(int i) {
        return rowValue.get(i);
    }

    public List<Value> getAllColumn() {
        return rowValue;
    }

    @Override
    public Value[] getColumns() {
        return rowValue.toArray(new Value[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SlothRow)) {
            return false;
        }
        SlothRow slothRow = (SlothRow) o;
        return rowValue.equals(slothRow.rowValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowValue);
    }
}
