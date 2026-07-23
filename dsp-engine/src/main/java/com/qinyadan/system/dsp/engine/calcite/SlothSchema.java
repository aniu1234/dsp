package com.qinyadan.system.dsp.engine.calcite;

import com.qinyadan.system.dsp.engine.meta.TableMeta;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;


@Slf4j
public class SlothSchema extends AbstractSchema {

    private final Map<String, Table> tables = new ConcurrentHashMap<>();

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

    public synchronized boolean dropTable(String tableName) {
        SlothTable slothTable = (SlothTable) tables.get(tableName);
        if (Objects.isNull(slothTable)) {
            return true;
        }

        slothTable.getSlothTableEngine().close();
        Path stagedPath = stageTableDirectory(slothTable);
        try {
            TableMeta.INSTANCE.deleteTable(schemaName, tableName);
            tables.remove(tableName);
            calciteSchema.removeTable(tableName);
        } catch (RuntimeException e) {
            restoreTableDirectory(stagedPath, slothTable);
            slothTable.initTableEngine();
            throw e;
        }
        deleteStagedDirectory(stagedPath);
        return true;
    }

    public synchronized boolean addTable(String tableName, SlothTable slothTable) {
        if (tables.containsKey(tableName)) {
            throw new IllegalStateException("Table already exists: " + schemaName + "." + tableName);
        }
        try {
            TableMeta.INSTANCE.addTable(schemaName, slothTable);
            tables.put(tableName, slothTable);
            calciteSchema.add(tableName, slothTable);
            return true;
        } catch (RuntimeException e) {
            tables.remove(tableName);
            calciteSchema.removeTable(tableName);
            try {
                TableMeta.INSTANCE.deleteTable(schemaName, tableName);
            } catch (RuntimeException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            slothTable.getSlothTableEngine().close();
            slothTable.getSlothTableEngine().removeData();
            throw e;
        }
    }

    public void restoreFromDb(SlothTable slothTable) {
        final String tableName = slothTable.getTableName();

        tables.put(tableName, slothTable);
        calciteSchema.add(tableName, slothTable);
    }

    public void dropTableInSchema() {
        new ArrayList<>(tables.keySet()).forEach(this::dropTable);
    }

    public void closeTables() {
        new ArrayList<>(tables.values()).forEach(table -> {
            SlothTable slothTable = (SlothTable) table;
            if (slothTable.getSlothTableEngine() != null) {
                slothTable.getSlothTableEngine().close();
            }
        });
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

    private Path stageTableDirectory(SlothTable table) {
        Path tablePath = Paths.get(table.buildTableEnginePath());
        if (!Files.exists(tablePath)) {
            return null;
        }
        Path stagedPath = tablePath.resolveSibling(tablePath.getFileName()
                + ".dropping-" + UUID.randomUUID());
        try {
            try {
                return Files.move(tablePath, stagedPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                return Files.move(tablePath, stagedPath);
            }
        } catch (IOException e) {
            table.initTableEngine();
            throw new IllegalStateException("Cannot stage table data for deletion: " + tablePath, e);
        }
    }

    private void restoreTableDirectory(Path stagedPath, SlothTable table) {
        if (stagedPath == null || !Files.exists(stagedPath)) {
            return;
        }
        Path tablePath = Paths.get(table.buildTableEnginePath());
        try {
            Files.move(stagedPath, tablePath);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot restore table data after failed drop: " + tablePath, e);
        }
    }

    private void deleteStagedDirectory(Path stagedPath) {
        if (stagedPath == null) {
            return;
        }
        try {
            FileUtils.deleteDirectory(stagedPath.toFile());
        } catch (IOException e) {
            log.warn("Logical table drop succeeded but staged data could not be deleted: {}",
                    stagedPath, e);
        }
    }

}
