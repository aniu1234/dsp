package com.qinyadan.system.dsp.storage.lucene;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import com.qinyadan.system.dsp.storage.QueryContext;
import com.qinyadan.system.dsp.storage.StorageEngine;
import com.qinyadan.system.dsp.storage.parser.SlothTableEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.qinyadan.system.dsp.runtime.engine.data.type.DataType.Precedence.*;


@Slf4j
public class LuceneStorageEngine implements StorageEngine {

    private String storagePath = "./dsp";

    // todo 元数据管理引擎
    private SlothTableEngine slothTableEngine ;

    private IndexWriter indexWriter;
    private IndexReader indexReader;
    private SearcherManager searcherManager;

    private boolean readOnly;

    private volatile int dataNumUncommited = 0;
    private volatile long lastFlushTime = System.currentTimeMillis();

    public LuceneStorageEngine (SlothTableEngine slothTableEngine){
        this.slothTableEngine = slothTableEngine;
    }

    public LuceneStorageEngine(String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public void init() {
        IndexWriterConfig conf = new IndexWriterConfig();
        try {
            indexWriter = new IndexWriter(new NIOFSDirectory(Paths.get(storagePath)), conf);
            DirectoryReader reader = DirectoryReader.open(indexWriter);
            indexReader = new SlothFilterDirectoryReader(reader,
                    new SlothFilterDirectoryReader.SubReaderWrapper(1));

            searcherManager = new SearcherManager(indexWriter, new SearcherFactory());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {

                    //TODO only this can generate segment file, or when restart
                    // data will lost
                    indexWriter.commit();
                } catch (Exception e) {
                    //ignore
                    log.error(e.getMessage());
                }
            }));
        } catch (IOException e) {
            log.error(Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean insert(List<List<Value>> rows) throws IOException {

        if (readOnly) {
            log.error("Storage engine is mark read only, can't flush");
            return false;
        }

        // async, 5 seconds later, data can be query
        for (List<Value> row : rows) {
            final Document document = rowToDocument(row);
            indexWriter.addDocument(document);
        }

        dataNumUncommited = dataNumUncommited + rows.size();
        return true;
    }

    private void updateIndexWriterAndReader() throws IOException {
        log.info("Start to flush, current thread = {}", Thread.currentThread());

        indexWriter.flush();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        indexReader = new SlothFilterDirectoryReader(reader,
                new SlothFilterDirectoryReader.SubReaderWrapper(1));


        searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
    }

    private Document rowToDocument(List<Value> row) {
        final Document document = new Document();

        final List<String> columnNames = slothTableEngine.getColumnNames();
        final Map<String, DataType> dataTypeList = slothTableEngine.getColumnAndDataType();

        //TODO 目测lucene 当前无法存NULL值，NULL值需要自已处理
        // NULL 映射某个固定的值

        /**
         * Store 'NULL' to String null
         *
         * for number type, minimum number of the type represent null value
         */
        for (int i = 0; i < row.size(); i++) {
            final Value value = row.get(i);
            final String columnName = columnNames.get(i);
            final DataType dataType = dataTypeList.get(columnName);

            if (BYTE.equals(dataType.precedence()) || SHORT.equals(dataType.precedence()) || INTEGER.equals(dataType.precedence())) {
                int content = value.isNull() ? Integer.MIN_VALUE : value.intValue();
                document.add(new IntPoint(columnName, content));
                document.add(new StoredField(columnName, content));
            } else if (LONG.equals(dataType.precedence())) {
                long content = value.isNull() ? Long.MIN_VALUE : value.longValue();
                document.add(new LongPoint(columnName, content));
                document.add(new StoredField(columnName, content));
            } else if (FLOAT.equals(dataType.precedence())) {
                float content = value.isNull() ? Float.MIN_VALUE : value.floatValue();
                document.add(new FloatPoint(columnName, content));
                document.add(new StoredField(columnName, content));
            } else if (DOUBLE.equals(dataType.precedence())) {
                double content = value.isNull() ? Double.MIN_VALUE : value.doubleValue();
                document.add(new DoublePoint(columnName, content));
                document.add(new StoredField(columnName, content));
            } else if (STRING.equals(dataType.precedence())) {
                //String values do not need to add store field
                String content = value.isNull() ? "NULL" : value.stringValue();
                document.add(new StringField(columnName, content, Field.Store.YES));
            } else if (DATE.equals(dataType.precedence())) {
                long content;
                if (value.isNull()) {
                    content = Long.MIN_VALUE;
                } else {
                    Date date = (Date) value.getValueByType();
                    if (Objects.isNull(date)) {
                        content = Long.MIN_VALUE;
                    } else {
                        content = date.getTime();
                    }
                }
                document.add(new LongPoint(columnName, content));
                document.add(new StoredField(columnName, content));

            } else {
                //maybe time/datetime/timestamp
                long content = value.isNull() ? Long.MIN_VALUE : value.longValue();
                document.add(new LongPoint(columnName, content));
                document.add(new StoredField(columnName, content));
            }
        }

        return document;
    }

    private SlothRow documentToRow(Document document) {

        final Map<String, DataType> columnAndDataType = slothTableEngine.getColumnAndDataType();
        //TODO 可能只select部分列，目前这里是选择全部的列，效率不太好
        List<Value> rs = Lists.newArrayList();
        final List<IndexableField> fields = document.getFields();
        for (int i = 0; i < columnAndDataType.size(); i++) {
            IndexableField field = fields.get(i);

            String columneName = field.name();
            DataType dataType = columnAndDataType.get(columneName);

            Value v;
            if (dataType.precedence() == STRING) {
                final String stringValue = field.stringValue();
                v = new Value(Objects.equals("NULL", stringValue) ? null : stringValue, dataType);
            } else if (dataType.precedence() == INTEGER || dataType.precedence() == SHORT || dataType.precedence() == BYTE) {
                Integer numericValue = (Integer) field.numericValue();
                v = new Value(Integer.MIN_VALUE == numericValue ? null : numericValue, dataType);
            } else if (dataType.precedence() == LONG) {
                Long numericValue = (Long) field.numericValue();
                v = new Value(Long.MIN_VALUE == numericValue ? null : numericValue, dataType);
            } else if (dataType.precedence() == FLOAT) {
                Float numericValue = (Float) field.numericValue();
                v = new Value(Float.MIN_VALUE == numericValue ? null : numericValue, dataType);
            } else if (dataType.precedence() == DOUBLE) {
                Double numericValue = (Double) field.numericValue();
                v = new Value(Double.MIN_VALUE == numericValue ? null : numericValue, dataType);
            } else if (dataType.precedence() == DATE) {
                Long numericValue = (Long) field.numericValue();
                v = new Value(Long.MIN_VALUE == numericValue ? null : numericValue, dataType);
            } else {
                Long numericValue = (Long) field.numericValue();
                v = new Value(Long.MIN_VALUE == numericValue ? null : numericValue, dataType);
            }
            rs.add(v);

        }

        return new SlothRow(rs);
    }

    @Override
    public Iterator<SlothRow> query(QueryContext queryContext) throws IOException {

        final IndexSearcher searcher = new IndexSearcher(indexReader);
        //final IndexSearcher searcher = searcherManager.acquire();
        //TODO, MAX_VALLUE will show down the query
        // TODO 通过 queryContext 初始化 下面的qeruy查询
        Query query = new MatchAllDocsQuery();
        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);


        //这里太丑陋了, 需要好好优化一下
        return Arrays.stream(topDocs.scoreDocs).parallel()
                .map(scoreDoc -> {
                    try {
                        return indexReader.document(scoreDoc.doc, queryContext.getColumnNames());
                    } catch (IOException e) {
                        log.error("get doc '{}' meets error:", scoreDoc.doc, e);
                        throw new RuntimeException(e);
                    }
                })
                .map(this::documentToRow)
                .iterator();
    }

    @Override
    public void close() {
        try {
            indexWriter.close();
            indexReader.close();
        } catch (IOException e) {
            //ingore exception
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public boolean readOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public void flush() {
        //todo make config

        if (readOnly) {
            log.error("Storage engine is mark read only, can't flush");
            return;
        }

        // Can use indexWriter.hasUncommittedChanges()
        if (dataNumUncommited > 0) {
            try {
                updateIndexWriterAndReader();
                dataNumUncommited = 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean shouldFlush() {
        //at last after 1s we should flush data
        return dataNumUncommited > 0 || System.currentTimeMillis() - lastFlushTime > 1000;
    }
}
