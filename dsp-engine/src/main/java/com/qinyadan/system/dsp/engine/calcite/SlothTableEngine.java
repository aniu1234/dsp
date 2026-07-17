package com.qinyadan.system.dsp.engine.calcite;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.core.data.RowImpl;
import com.qinyadan.system.dsp.engine.LifeCycle;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.data.value.Value;
import com.qinyadan.system.dsp.engine.util.TypeConversionUtils;
import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.EngineRegistry;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.WriteResult;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 元数据管理引擎
 */
@Slf4j
public class SlothTableEngine implements LifeCycle {

    /**
     * Sloth Table instance
     */
    @Getter
    private final SlothTable slothTable;

    /**
     * Column name and type, maybe this should change according to variables
     */
    @Getter
    private final Map<String, DataType> columnAndDataType = Maps.newHashMap();


    /**
     * All column name
     */
    @Getter
    private final List<String> columnNames = Lists.newArrayList();

    /**
     * 　引擎分片
     * MultiShard instance
     */
    @Getter
    private List<StorageEngine> storageEngines;


    public SlothTableEngine(SlothTable slothTable) {
        this.slothTable = slothTable;
        init();
    }


    /**
     * @param values
     */
    public void insert(List<List<Value>> values) {
        int shard = ThreadLocalRandom.current().nextInt(slothTable.getShardNum());
        final StorageEngine storageEngine = storageEngines.get(shard);

        try {
            com.qinyadan.system.dsp.core.data.Row[] rows = values.stream()
                    .map(this::toStorageRow)
                    .toArray(com.qinyadan.system.dsp.core.data.Row[]::new);
            WriteResult result = storageEngine.append(rows);
            if (result.getInserted() != rows.length) {
                throw new IllegalStateException("Storage engine inserted " + result.getInserted()
                        + " of " + rows.length + " rows");
            }
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
            storageEngine.setReadOnly(true);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param queryContext
     * @return
     */
    public Iterator<SlothRow> search(QueryContext queryContext) {

        List<Iterator<SlothRow>> searchIts = Lists.newArrayList();
        final List<String> resultColumns = selectedColumns(queryContext);

        try {
            for (StorageEngine storageEngine : storageEngines) {
                searchIts.add(toEngineRows(storageEngine.scan(queryContext), resultColumns));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new MergeIterator<>(searchIts);
    }


    @Override
    public void init() {
        resolveType();
        loadData();
    }

    @Override
    public void close() {
        storageEngines.forEach(StorageEngine::close);
    }


    private void loadData() {
        storageEngines = Lists.newArrayList();
        if (slothTable.getShardNum() < 1) {
            throw new IllegalArgumentException("Table must have at least one shard: "
                    + slothTable.getTableName());
        }
        final String basePath = slothTable.buildTableEnginePath();
        final List<ColumnInfo> columns = storageColumns();
        try {
            for (int shard = 0; shard < slothTable.getShardNum(); shard++) {
                String shardPath = basePath + File.separator + shard;
                EngineConfig config = new EngineConfig(shardPath, columns,
                        Collections.<String, Object>singletonMap("shard", shard));
                StorageEngine storageEngine = EngineRegistry.create("lucene", config);
                storageEngines.add(storageEngine);
            }
        } catch (RuntimeException e) {
            storageEngines.forEach(StorageEngine::close);
            storageEngines.clear();
            throw e;
        }
    }

    private com.qinyadan.system.dsp.core.data.Row toStorageRow(List<Value> values) {
        if (values.size() != columnNames.size()) {
            throw new IllegalArgumentException("Expected " + columnNames.size()
                    + " columns but got " + values.size());
        }
        com.qinyadan.system.dsp.core.data.value.Value[] result =
                new com.qinyadan.system.dsp.core.data.value.Value[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Value value = values.get(i);
            DataType type = columnAndDataType.get(columnNames.get(i));
            result[i] = new com.qinyadan.system.dsp.core.data.value.Value(
                    value == null ? null : value.getValue(), toStorageType(type));
        }
        return RowImpl.of(result);
    }

    private Iterator<SlothRow> toEngineRows(
            final Iterator<com.qinyadan.system.dsp.core.data.Row> rows,
            final List<String> resultColumns) {
        return new Iterator<SlothRow>() {
            @Override
            public boolean hasNext() {
                return rows.hasNext();
            }

            @Override
            public SlothRow next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                com.qinyadan.system.dsp.core.data.Row row = rows.next();
                if (row.columnSize() != resultColumns.size()) {
                    throw new IllegalStateException("Storage row has " + row.columnSize()
                            + " columns, expected " + resultColumns.size());
                }
                List<Value> values = new ArrayList<>(row.columnSize());
                for (int i = 0; i < row.columnSize(); i++) {
                    com.qinyadan.system.dsp.core.data.value.Value value = row.getColumn(i);
                    values.add(new Value(value.getValue(),
                            columnAndDataType.get(resultColumns.get(i))));
                }
                return new SlothRow(values);
            }
        };
    }

    private List<String> selectedColumns(QueryContext queryContext) {
        Set<String> requested = queryContext == null ? null : queryContext.getColumnNames();
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(columnNames);
        }
        Set<String> requestedCopy = new HashSet<>(requested);
        List<String> result = new ArrayList<>();
        for (String column : columnNames) {
            if (requestedCopy.contains(column)) {
                result.add(column);
            }
        }
        return result;
    }

    private List<ColumnInfo> storageColumns() {
        List<ColumnInfo> result = new ArrayList<>(slothTable.getColumns().size());
        for (SlothColumn column : slothTable.getColumns()) {
            EnhanceSlothColumn definition = column.getColumnType();
            result.add(new ColumnInfo(column.getColumnName(),
                    toStorageType(columnAndDataType.get(column.getColumnName())),
                    definition.isNullable(), definition.getDefalutValue(),
                    definition.getColumnComment()));
        }
        return result;
    }

    private static com.qinyadan.system.dsp.core.data.type.DataType<?> toStorageType(DataType type) {
        switch (type.precedence()) {
            case BYTE:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.BYTE;
            case BOOLEAN:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.BOOLEAN;
            case SHORT:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.SHORT;
            case INTEGER:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.INTEGER;
            case LONG:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.LONG;
            case FLOAT:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.FLOAT;
            case DOUBLE:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.DOUBLE;
            case DATE:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.DATE;
            case TIMESTAMP:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.TIMESTAMP;
            case TIMESTAMP_WITH_TIME_ZONE:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.TIMESTAMPZ;
            case STRING:
                return com.qinyadan.system.dsp.core.data.type.DataTypes.STRING;
            default:
                throw new IllegalArgumentException("Unsupported storage type: " + type);
        }
    }

    public void removeData() {
        final String basePath = slothTable.buildTableEnginePath();
        try {
            for (int shard = 0; shard < slothTable.getShardNum(); shard++) {
                String shardPath = basePath + File.separator + shard;
                log.info("try to delete path '{}'", shardPath);
                FileUtils.deleteDirectory(new File(shardPath));
                log.info("delete path '{}' succeed", shardPath);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void resolveType() {
        slothTable.getColumns().forEach(column -> {
            final SqlTypeName sqlTypeName = column.getColumnType().getColumnType();
            columnAndDataType.put(column.getColumnName(),
                    TypeConversionUtils.getBySqlTypeName(sqlTypeName));
            columnNames.add(column.getColumnName());
        });
    }
}
