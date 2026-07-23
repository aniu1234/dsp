package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.meta.SchemaMeta;
import com.qinyadan.system.dsp.engine.meta.TableMeta;
import com.qinyadan.system.dsp.engine.constant.FileConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.Table;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 加载所有的DB  schema
 */
@Slf4j
public class SlothSchemaHolder implements LifeCycle {

    private Map<String, SlothSchema> schemaMap;

    public static final SlothSchemaHolder INSTANCE = new SlothSchemaHolder();

    public SlothSchemaHolder() {
        this.schemaMap = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void init() {
        SchemaMeta.INSTANCE.allSchema().forEach(schemaName -> {
            SlothSchema schema = restoreSchema(schemaName);
            TableMeta.INSTANCE.getAllTableInDb(schema).forEach(table -> {
                table.initTableEngine();
                schema.restoreFromDb(table);
            });
        });
    }

    @Override
    public void close() {
        schemaMap.values().forEach(SlothSchema::closeTables);
    }

    public synchronized SlothSchema registerSchema(String schemaName) {
        if (schemaMap.containsKey(schemaName)) {
            throw new IllegalStateException("Schema already exists: " + schemaName);
        }
        SchemaMeta.INSTANCE.addSchema(schemaName);
        try {
            return addSchemaToMemory(schemaName);
        } catch (RuntimeException e) {
            SchemaMeta.INSTANCE.dropSchema(schemaName);
            throw e;
        }
    }

    private SlothSchema restoreSchema(String schemaName) {
        SlothSchema existing = schemaMap.get(schemaName);
        if (existing != null) {
            return existing;
        }
        return addSchemaToMemory(schemaName);
    }

    private SlothSchema addSchemaToMemory(String schemaName) {
        final SlothSchema slothSchema = new SlothSchema(schemaName);
        schemaMap.put(schemaName, slothSchema);
        try {
            CalciteSchema schema = ParserFactory.getCatalogReader().getRootSchema()
                    .add(schemaName, slothSchema);
            slothSchema.setCalciteSchema(schema);
            return slothSchema;
        } catch (RuntimeException e) {
            schemaMap.remove(schemaName);
            throw e;
        }
    }

    public synchronized boolean removeSchema(String schemaName) {
        SlothSchema schema = SlothSchemaHolder.INSTANCE.getSlothSchema(schemaName);
        if (schema == null) {
            return false;
        }
        schema.closeTables();
        Path stagedPath = stageSchemaDirectory(schemaName, schema);
        try {
            SchemaMeta.INSTANCE.dropSchema(schemaName);
            schemaMap.remove(schemaName);
            ParserFactory.getCatalogReader().getRootSchema().removeSubSchema(schemaName);
        } catch (RuntimeException e) {
            restoreSchemaDirectory(schemaName, stagedPath);
            reopenTables(schema);
            throw e;
        }
        deleteStagedDirectory(stagedPath);
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

    private Path stageSchemaDirectory(String schemaName, SlothSchema schema) {
        Path schemaPath = Paths.get(FileConstants.getTableFileLocation(), schemaName);
        if (!Files.exists(schemaPath)) {
            return null;
        }
        Path stagedPath = schemaPath.resolveSibling(schemaPath.getFileName()
                + ".dropping-" + UUID.randomUUID());
        try {
            try {
                return Files.move(schemaPath, stagedPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                return Files.move(schemaPath, stagedPath);
            }
        } catch (IOException e) {
            reopenTables(schema);
            throw new IllegalStateException("Cannot stage schema data for deletion: " + schemaPath, e);
        }
    }

    private void restoreSchemaDirectory(String schemaName, Path stagedPath) {
        if (stagedPath == null || !Files.exists(stagedPath)) {
            return;
        }
        Path schemaPath = Paths.get(FileConstants.getTableFileLocation(), schemaName);
        try {
            Files.move(stagedPath, schemaPath);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot restore schema data after failed drop: " + schemaPath, e);
        }
    }

    private void reopenTables(SlothSchema schema) {
        for (Table table : schema.getAllTable()) {
            ((SlothTable) table).initTableEngine();
        }
    }

    private void deleteStagedDirectory(Path stagedPath) {
        if (stagedPath == null) {
            return;
        }
        try {
            FileUtils.deleteDirectory(stagedPath.toFile());
        } catch (IOException e) {
            log.warn("Logical schema drop succeeded but staged data could not be deleted: {}",
                    stagedPath, e);
        }
    }
}
