package com.qinyadan.system.dsp.runtime.engine;

import com.qinyadan.system.dsp.runtime.engine.column.AbstractColumn;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import org.apache.calcite.schema.Table;

import java.util.Iterator;
import java.util.List;


public class RealTimeEngine extends AbstractStorageEngine {

    /**
     * @param table
     * @param columns
     * @param storePath, for realtime engine, storage path is checkpoint path
     */
    public RealTimeEngine(Table table, List<AbstractColumn> columns, String storePath) {
        super(table, columns, storePath);
    }

    @Override
    public boolean insert(List<List<Value>> rows) {
        rows.forEach(row -> {
            for (int i = 0; i < columns.size(); i++) {
                Value value = row.get(i);
                AbstractColumn abstractColumn = columns.get(i);
                abstractColumn.put(value.getValueByType());
            }
        });

        return true;

    }

    @Override
    public Iterator<List<Value>> query() {
        return null;
    }
}
