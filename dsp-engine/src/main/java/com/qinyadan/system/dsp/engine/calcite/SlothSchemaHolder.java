package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.meta.SchemaMeta;
import com.qinyadan.system.dsp.engine.meta.TableMeta;
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
        // TableMeta expects base-storage's SlothSchema/SlothTable types.
        // Skip DB restore for now — tables are registered via registerSchema/addTable.
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
