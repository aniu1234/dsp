package com.qinyadan.system.dsp.storage.lucene;

import com.google.common.base.Throwables;
import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.RowImpl;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.WriteResult;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.qinyadan.system.dsp.core.data.type.DataType.Precedence.*;

/**
 * Lucene-backed storage engine implementing the {@link StorageEngine} interface.
 * Supports insert, scan, flush, and close operations on a Lucene index.
 */
public class LuceneStorageEngine implements StorageEngine {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneStorageEngine.class);

    private final String storagePath;
    private final List<ColumnInfo> columns;
    private final Map<String, DataType<?>> typeMap;
    private final LuceneDocumentMapper mapper;

    private IndexWriter indexWriter;
    private IndexReader indexReader;
    private SearcherManager searcherManager;

    private boolean readOnly;
    private volatile int dataNumUncommited = 0;
    private volatile long lastFlushTime = System.currentTimeMillis();

    public LuceneStorageEngine(String storagePath, List<ColumnInfo> columns, Map<String, DataType<?>> typeMap) {
        this.storagePath = storagePath;
        this.columns = columns;
        this.typeMap = typeMap;
        this.mapper = new LuceneDocumentMapper(columns, typeMap);
    }

    /**
     * Initialize the Lucene index writer and reader.
     */
    public void init() {
        IndexWriterConfig conf = new IndexWriterConfig();
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            indexWriter = new IndexWriter(new NIOFSDirectory(Paths.get(storagePath)), conf);
            DirectoryReader reader = DirectoryReader.open(indexWriter);
            indexReader = new SlothFilterDirectoryReader(reader,
                    new SlothFilterDirectoryReader.SubReaderWrapper(1));

            searcherManager = new SearcherManager(indexWriter, new SearcherFactory());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    indexWriter.commit();
                } catch (Exception e) {
                    LOG.error("Shutdown hook commit failed", e);
                }
            }));
        } catch (IOException e) {
            LOG.error(Throwables.getStackTraceAsString(e));
            throw new RuntimeException("Failed to init LuceneStorageEngine at " + storagePath, e);
        }
    }

    @Override
    public WriteResult append(Row... rows) throws IOException {
        if (readOnly) {
            LOG.error("Storage engine is marked read only, can't insert");
            return new WriteResult(0, System.currentTimeMillis());
        }

        for (Row row : rows) {
            Document document = mapper.toRow(row);
            indexWriter.addDocument(document);
        }

        dataNumUncommited += rows.length;
        return new WriteResult(rows.length, System.currentTimeMillis());
    }

    @Override
    public <R> Iterator<R> scan(QueryContext queryContext) throws IOException {
        IndexSearcher searcher = new IndexSearcher(indexReader);
        Query query = new MatchAllDocsQuery();
        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

        Set<String> requestedColumns = queryContext.getColumnNames();
        List<R> result = new ArrayList<>(topDocs.scoreDocs.length);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = requestedColumns != null && !requestedColumns.isEmpty()
                    ? indexReader.document(scoreDoc.doc, requestedColumns)
                    : indexReader.document(scoreDoc.doc);
            result.add((R) documentToRow(doc));
        }
        return result.iterator();
    }

    private Row documentToRow(Document document) {
        List<Value> values = new ArrayList<>();
        List<IndexableField> fields = document.getFields();

        for (int i = 0; i < columns.size() && i < fields.size(); i++) {
            IndexableField field = fields.get(i);
            ColumnInfo col = columns.get(i);
            DataType<?> type = col.getType();

            values.add(fieldToValue(field, type));
        }
        return RowImpl.of(values.toArray(new Value[0]));
    }

    private Value fieldToValue(IndexableField field, DataType<?> type) {
        Number numericValue = field.numericValue();
        if (numericValue == null) {
            String strVal = field.stringValue();
            return new Value("NULL".equals(strVal) ? null : strVal, type);
        }

        Object val;
        switch (type.precedence()) {
            case BYTE:
            case SHORT:
            case INTEGER:
                val = numericValue.intValue();
                break;
            case LONG:
                val = numericValue.longValue();
                break;
            case FLOAT:
                val = numericValue.floatValue();
                break;
            case DOUBLE:
                val = numericValue.doubleValue();
                break;
            default:
                val = numericValue.longValue();
                break;
        }

        return new Value(val, type);
    }

    private void updateIndexWriterAndReader() throws IOException {
        LOG.info("Starting flush, current thread = {}", Thread.currentThread());
        indexWriter.flush();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        indexReader = new SlothFilterDirectoryReader(reader,
                new SlothFilterDirectoryReader.SubReaderWrapper(1));

        searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
    }

    @Override
    public long estimateRowCount() {
        return indexReader.numDocs();
    }

    @Override
    public void flush() {
        if (readOnly) {
            LOG.error("Storage engine is marked read only, can't flush");
            return;
        }

        if (dataNumUncommited > 0) {
            try {
                updateIndexWriterAndReader();
                dataNumUncommited = 0;
            } catch (IOException e) {
                throw new RuntimeException("Flush failed", e);
            }
        }
    }

    @Override
    public boolean shouldFlush() {
        return dataNumUncommited > 0 || System.currentTimeMillis() - lastFlushTime > 1000;
    }

    @Override
    public void close() {
        try {
            if (indexWriter != null) indexWriter.close();
            if (indexReader != null) indexReader.close();
        } catch (IOException e) {
            LOG.error(Throwables.getStackTraceAsString(e));
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
}
