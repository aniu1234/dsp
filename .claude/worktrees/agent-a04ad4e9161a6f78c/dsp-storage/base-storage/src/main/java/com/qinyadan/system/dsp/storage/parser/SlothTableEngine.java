package com.qinyadan.system.dsp.storage.parser;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.runtime.LifeCycle;
import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import com.qinyadan.system.dsp.runtime.util.TypeConversionUtils;
import com.qinyadan.system.dsp.storage.QueryContext;
import com.qinyadan.system.dsp.storage.StorageEngine;
import com.qinyadan.system.dsp.storage.lucene.LuceneStorageEngine;
import com.qinyadan.system.dsp.storage.parser.util.SlothMergeIterator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;


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
        int shard = new Random().nextInt(slothTable.getShardNum());
        final StorageEngine storageEngine = storageEngines.get(shard);

        try {
            storageEngine.insert(values);
        } catch (Exception e) {
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

        try {
            for (StorageEngine storageEngine : storageEngines) {
                searchIts.add(storageEngine.query(queryContext));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new SlothMergeIterator<>(searchIts);
    }


    @Override
    public void init() {
        resolveType();
        loadData();
    }

    @Override
    public void close() {
        storageEngines.forEach(LifeCycle::close);
    }


    private void loadData() {
        storageEngines = Lists.newArrayList();
        final String basePath = slothTable.buildTableEnginePath();
        for (int shard = 0; shard < slothTable.getShardNum(); shard++) {
            String shardPath = basePath + File.separator + shard;

            StorageEngine storageEngine = new LuceneStorageEngine(this);
            storageEngine.init();

            storageEngines.add(storageEngine);
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
