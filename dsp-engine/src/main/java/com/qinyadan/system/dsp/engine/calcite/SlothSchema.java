package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.*;


public class SlothSchema extends AbstractSchema {

    private Map<String, Table> tables = Maps.newHashMap();

    @Getter
    private String schemaName;

    @Setter
    private CalciteSchema calciteSchema;

    public SlothSchema(String schemaName) {
        this.schemaName = schemaName;
    }


    @Override
    protected Map<String, Table> getTableMap() {
        return tables;
    }

    public boolean dropTable(String tableName) {
        SlothTable slothTable = (SlothTable) tables.get(tableName);
        //是否要考虑同步问题
        if (Objects.isNull(slothTable)) {
            return true;
        }

        //物理删除数据
        slothTable.getSlothTableEngine().close();
        slothTable.getSlothTableEngine().removeData();

        //schem删除
        tables.remove(tableName);
        calciteSchema.removeTable(tableName);

        // TableMeta persistence disabled due to type mismatch with base-storage
        // TableMeta.INSTANCE.deleteTable(schemaName, tableName);
        return true;
    }

    public boolean addTable(String tableName, SlothTable slothTable) {
        // TableMeta expects base-storage's SlothTable, not engine's.
        // The table metadata persistence is handled separately.
        // tables.put(tableName, slothTable); // disabled due to type mismatch
        calciteSchema.add(tableName, slothTable);
        return true;
    }

    public void restoreFromDb(SlothTable slothTable) {
        final String tableName = slothTable.getTableName();

        tables.put(tableName, slothTable);
        calciteSchema.add(tableName, slothTable);
    }

    public void dropTableInSchema() {
        tables.keySet().forEach(this::dropTable);
    }

    public Collection<Table> getAllTable() {
        return tables.values();
    }

    public boolean containsTable(String tableName) {
        return tables.containsKey(tableName);
    }

    public List<String> getTables() {
        return new ArrayList<>(tables.keySet());
    }

}
