package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.WriteResult;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.NoSuchElementException;

/**
 * Lucene-backed implementation of the public storage API.
 */
public class LuceneStorageEngine implements StorageEngine {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneStorageEngine.class);

    private final String storagePath;
    private final List<ColumnInfo> columns;
    private final LuceneDocumentMapper mapper;
    private final AtomicInteger uncommittedRows = new AtomicInteger();

    private NIOFSDirectory directory;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private volatile boolean readOnly;
    private volatile boolean closed;
    private volatile long lastFlushTime = System.currentTimeMillis();

    public LuceneStorageEngine(String storagePath, List<ColumnInfo> columns) {
        if (storagePath == null || columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Storage path and columns are required");
        }
        this.storagePath = storagePath;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        Set<String> names = new HashSet<>();
        for (ColumnInfo column : columns) {
            if (column == null || column.getName() == null || column.getType() == null
                    || !names.add(column.getName())) {
                throw new IllegalArgumentException("Column names and types must be non-null and unique");
            }
        }
        this.mapper = new LuceneDocumentMapper(this.columns);
    }

    public synchronized void init() {
        if (indexWriter != null) {
            return;
        }
        try {
            Files.createDirectories(Paths.get(storagePath));
            directory = new NIOFSDirectory(Paths.get(storagePath));
            indexWriter = new IndexWriter(directory, new IndexWriterConfig());
            searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
        } catch (IOException e) {
            close();
            throw new RuntimeException("Failed to initialize Lucene storage at " + storagePath, e);
        }
    }

    @Override
    public synchronized WriteResult append(Row... rows) throws IOException {
        ensureOpen();
        if (readOnly) {
            throw new IllegalStateException("Lucene storage engine is read only: " + storagePath);
        }
        if (rows == null || rows.length == 0) {
            return new WriteResult(0, System.currentTimeMillis());
        }

        List<Document> documents = new ArrayList<>(rows.length);
        for (Row row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("Rows to append must not contain null");
            }
            documents.add(mapper.toDocument(row));
        }
        indexWriter.addDocuments(documents);
        uncommittedRows.addAndGet(rows.length);
        searcherManager.maybeRefreshBlocking();
        return new WriteResult(rows.length, System.currentTimeMillis());
    }

    @Override
    public Iterator<Row> scan(QueryContext queryContext) throws IOException {
        ensureOpen();
        Set<String> requestedColumns = queryContext == null ? null : queryContext.getColumnNames();
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            requestedColumns = new LinkedHashSet<>();
            for (ColumnInfo column : columns) {
                requestedColumns.add(column.getName());
            }
        } else {
            requestedColumns = new LinkedHashSet<>(requestedColumns);
        }
        Set<String> knownColumns = new HashSet<>();
        for (ColumnInfo column : columns) {
            knownColumns.add(column.getName());
        }
        if (!knownColumns.containsAll(requestedColumns)) {
            Set<String> unknown = new HashSet<>(requestedColumns);
            unknown.removeAll(knownColumns);
            throw new IllegalArgumentException("Unknown storage columns: " + unknown);
        }

        Set<String> storedFields = new HashSet<>(requestedColumns);
        for (String column : requestedColumns) {
            storedFields.add(LuceneDocumentMapper.nullMarker(column));
            storedFields.add(LuceneDocumentMapper.legacyNullMarker(column));
        }
        return new PagedScanIterator(requestedColumns, storedFields, scanPageSize());
    }

    @Override
    public long estimateRowCount() {
        ensureOpen();
        IndexSearcher searcher = null;
        try {
            searcher = searcherManager.acquire();
            return searcher.getIndexReader().numDocs();
        } catch (IOException e) {
            throw new RuntimeException("Failed to estimate Lucene row count", e);
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) {
                    LOG.warn("Failed to release Lucene searcher", e);
                }
            }
        }
    }

    @Override
    public synchronized void flush() {
        ensureOpen();
        if (readOnly || uncommittedRows.get() == 0) {
            return;
        }
        try {
            indexWriter.commit();
            searcherManager.maybeRefreshBlocking();
            uncommittedRows.set(0);
            lastFlushTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException("Failed to flush Lucene storage at " + storagePath, e);
        }
    }

    @Override
    public boolean shouldFlush() {
        return !closed && uncommittedRows.get() > 0
                && System.currentTimeMillis() - lastFlushTime > 1000;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (indexWriter != null && !readOnly) {
            try {
                flush();
            } catch (RuntimeException e) {
                LOG.error("Failed to flush Lucene storage before closing", e);
            }
        }
        closeSearcherManager();
        closeIndexWriter();
        closeDirectory();
        closed = true;
    }

    @Override
    public boolean readOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private void ensureOpen() {
        if (closed || indexWriter == null || searcherManager == null) {
            throw new IllegalStateException("Lucene storage engine is not open: " + storagePath);
        }
    }

    private void closeSearcherManager() {
        if (searcherManager != null) {
            try {
                searcherManager.close();
            } catch (IOException e) {
                LOG.error("Failed to close Lucene searcher manager", e);
            }
        }
    }

    private void closeIndexWriter() {
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException e) {
                LOG.error("Failed to close Lucene index writer", e);
            }
        }
    }

    private void closeDirectory() {
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                LOG.error("Failed to close Lucene directory", e);
            }
        }
    }

    private int scanPageSize() {
        return DspConfiguration.load().getStorageScanPageSize();
    }

    private final class PagedScanIterator implements Iterator<Row> {

        private final Set<String> requestedColumns;
        private final Set<String> storedFields;
        private final int pageSize;
        private Iterator<Row> page = Collections.<Row>emptyList().iterator();
        private ScoreDoc searchAfter;
        private boolean exhausted;

        private PagedScanIterator(Set<String> requestedColumns, Set<String> storedFields,
                                  int pageSize) {
            this.requestedColumns = requestedColumns;
            this.storedFields = storedFields;
            this.pageSize = pageSize;
        }

        @Override
        public boolean hasNext() {
            if (page.hasNext()) {
                return true;
            }
            if (exhausted) {
                return false;
            }
            loadPage();
            return page.hasNext();
        }

        @Override
        public Row next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return page.next();
        }

        private void loadPage() {
            IndexSearcher searcher = null;
            try {
                searcher = searcherManager.acquire();
                ScoreDoc[] scoreDocs = searcher.searchAfter(
                        searchAfter, new MatchAllDocsQuery(), pageSize).scoreDocs;
                if (scoreDocs.length == 0) {
                    exhausted = true;
                    page = Collections.<Row>emptyList().iterator();
                    return;
                }
                List<Row> rows = new ArrayList<>(scoreDocs.length);
                for (ScoreDoc scoreDoc : scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc, storedFields);
                    rows.add(mapper.fromDocument(document, requestedColumns));
                }
                searchAfter = scoreDocs[scoreDocs.length - 1];
                exhausted = scoreDocs.length < pageSize;
                page = rows.iterator();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to scan Lucene storage at " + storagePath, e);
            } finally {
                if (searcher != null) {
                    try {
                        searcherManager.release(searcher);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to release Lucene searcher", e);
                    }
                }
            }
        }
    }
}
