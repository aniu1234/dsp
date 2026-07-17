package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.meta.SchemaMeta;
import com.qinyadan.system.dsp.engine.meta.TableMeta;
import org.apache.calcite.jdbc.CalciteSchema;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 加载所有的DB  schema
 */
public class SlothSchemaHolder implements LifeCycle {

    private Map<String, SlothSchema> schemaMap;

    public static final SlothSchemaHolder INSTANCE = new SlothSchemaHolder();

    public SlothSchemaHolder() {
        this.schemaMap = new ConcurrentHashMap<>();
    }

    @Override
    public void init() {
        SchemaMeta.INSTANCE.allSchema().forEach(schemaName -> {
            SlothSchema schema = registerSchema(schemaName);
            TableMeta.INSTANCE.getAllTableInDb(schema).forEach(schema::restoreFromDb);
        });
    }

    @Override
    public void close() {
        schemaMap.values().forEach(SlothSchema::closeTables);
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
        if (schema == null) {
            return false;
        }
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
