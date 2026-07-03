package com.qinyadan.system.dsp.storage.parser;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.runtime.LifeCycle;
import com.qinyadan.system.dsp.storage.meta.SchemaMeta;
import com.qinyadan.system.dsp.storage.meta.TableMeta;
import org.apache.calcite.jdbc.CalciteSchema;

import java.util.*;


/**
 * 加载所有的DB  schema
 */
public class SlothSchemaHolder implements LifeCycle {

    private Map<String, SlothSchema> schemaMap;

    public static final SlothSchemaHolder INSTANCE = new SlothSchemaHolder();

    public SlothSchemaHolder() {
        this.schemaMap = Maps.newHashMap();
    }

    @Override
    public void init() {
        Set<String> schemes = SchemaMeta.INSTANCE.allSchema();
        for (String schema : schemes) {
            SlothSchema slothSchema = registerSchema(schema);
            //register table
            List<SlothTable> slothTables = TableMeta.INSTANCE.getAllTableInDb(slothSchema);
            for (SlothTable slothTable : slothTables) {
                slothSchema.restoreFromDb(slothTable);
            }
        }
    }

    @Override
    public void close() {

    }

    public SlothSchema registerSchema(String schemaName) {

        //add db first
        if (SchemaMeta.INSTANCE.schemaIsOk()) {
            SchemaMeta.INSTANCE.addSchema(schemaName);
        }

        //then add in schema
        final SlothSchema slothSchema = new SlothSchema(schemaName);
        schemaMap.put(schemaName, slothSchema);
        CalciteSchema schema =
                ParserFactory.getCatalogReader().getRootSchema().add(schemaName, slothSchema);
        slothSchema.setCalciteSchema(schema);
        return slothSchema;
    }

    public boolean removeSchema(String schemaName) {
        SlothSchema schema = SlothSchemaHolder.INSTANCE.getSlothSchema(schemaName);
        schema.dropTableInSchema();

        schemaMap.remove(schemaName);
        //remove data in schema
        ParserFactory.getCatalogReader().getRootSchema()
                .removeSubSchema(schemaName);

        //remove data in db;
        SchemaMeta.INSTANCE.dropSchema(schemaName);
        return true;
    }

    public List<String> getAllSchemas() {
        return new ArrayList<>(schemaMap.keySet());
    }

    public boolean contains(String db) {
        return schemaMap.containsKey(db);
    }

    public SlothSchema getSlothSchema(String dbName) {
        return schemaMap.get(dbName);
    }

    public Collection<SlothSchema> getSchemaMap() {
        return schemaMap.values();
    }
}
