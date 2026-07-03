package com.qinyadan.system.dsp.runtime.engine;

import com.qinyadan.system.dsp.runtime.engine.column.AbstractColumn;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import org.apache.calcite.schema.Table;

import java.util.Iterator;
import java.util.List;


public class BlockEngine extends AbstractStorageEngine {

    public BlockEngine(Table table, List<AbstractColumn> columns, String storePath) {
        super(table, columns, storePath);
    }

    @Override
    public boolean insert(List<List<Value>> rows) {
        //block engine do nt support insert
        return false;
    }

    @Override
    public Iterator<List<Value>> query() {
        return null;
    }
}
