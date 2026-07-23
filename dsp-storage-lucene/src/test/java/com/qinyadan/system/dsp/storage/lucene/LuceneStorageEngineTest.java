package com.qinyadan.system.dsp.storage.lucene;

import com.qinyadan.system.dsp.core.data.Row;
import com.qinyadan.system.dsp.core.data.RowImpl;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.storage.api.EngineConfig;
import com.qinyadan.system.dsp.storage.api.EngineRegistry;
import com.qinyadan.system.dsp.storage.api.StorageEngine;
import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import com.qinyadan.system.dsp.storage.api.meta.CreateTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.MetadataStore;
import com.qinyadan.system.dsp.storage.api.meta.OpenTableRequest;
import com.qinyadan.system.dsp.storage.api.meta.TableInfo;
import com.qinyadan.system.dsp.storage.api.query.BaseQuery;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import com.qinyadan.system.dsp.storage.api.service.StorageManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LuceneStorageEngineTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void registryEngineIsImmediatelyVisibleAndPersistent() throws Exception {
        assertTrue(EngineRegistry.availableTypes().contains("lucene"));
        File index = temporaryFolder.newFolder("index");
        List<ColumnInfo> columns = columns();
        EngineConfig config = new EngineConfig(index.getAbsolutePath(), columns,
                Collections.<String, Object>emptyMap());

        StorageEngine engine = EngineRegistry.create("LUCENE", config);
        engine.append(
                RowImpl.of(new Value(Integer.MIN_VALUE, DataTypes.INTEGER),
                        new Value("NULL", DataTypes.STRING),
                        new Value(false, DataTypes.BOOLEAN)),
                RowImpl.of(new Value(null, DataTypes.INTEGER),
                        new Value(null, DataTypes.STRING),
                        new Value(null, DataTypes.BOOLEAN)));

        List<Row> rows = scanAll(engine, columns);
        assertEquals(2, rows.size());
        assertEquals(Integer.MIN_VALUE, rows.get(0).getColumn(0).intValue().intValue());
        assertEquals("NULL", rows.get(0).getColumn(1).stringValue());
        assertFalse(rows.get(0).getColumn(2).booleanValue());
        assertNull(rows.get(1).getColumn(0).getValue());
        assertNull(rows.get(1).getColumn(1).getValue());
        assertNull(rows.get(1).getColumn(2).getValue());
        assertEquals(2L, engine.estimateRowCount());
        engine.close();

        StorageEngine reopened = EngineRegistry.create("lucene", config);
        assertEquals(2L, reopened.estimateRowCount());
        assertEquals(2, scanAll(reopened, columns).size());
        reopened.close();
    }

    @Test
    public void storageManagerOpensEveryConfiguredShard() throws Exception {
        List<ColumnInfo> columns = columns();
        TableInfo table = new TableInfo("dsp", "test_db", "test_table",
                "lucene", 2, "", columns);
        MetadataStore metadata = metadata(table);
        File root = temporaryFolder.newFolder("manager");

        StorageManager manager = new StorageManager(metadata, root.toPath());
        List<StorageEngine> engines = manager.openTable(
                new OpenTableRequest("test_db", "test_table", columns));

        assertEquals(2, engines.size());
        assertEquals(Collections.singletonList("test_db.test_table"), manager.listOpenTables());
        assertTrue(new File(root, "test_db/test_table/0").isDirectory());
        assertTrue(new File(root, "test_db/test_table/1").isDirectory());
        manager.close();
        assertTrue(manager.listOpenTables().isEmpty());
    }

    @Test
    public void mapperReadsExplicitMarkersWrittenByThePreviousEngine() {
        List<ColumnInfo> columns = Collections.singletonList(
                new ColumnInfo("id", DataTypes.INTEGER, true, null, ""));
        LuceneDocumentMapper mapper = new LuceneDocumentMapper(columns);

        Document valueDocument = new Document();
        valueDocument.add(new StoredField("id", Integer.MIN_VALUE));
        valueDocument.add(new StoredField("id__dsp_null", 0));
        Row valueRow = mapper.fromDocument(valueDocument, Collections.singleton("id"));
        assertEquals(Integer.MIN_VALUE, valueRow.getColumn(0).intValue().intValue());

        Document nullDocument = new Document();
        nullDocument.add(new StoredField("id", Integer.MIN_VALUE));
        nullDocument.add(new StoredField("id__dsp_null", 1));
        Row nullRow = mapper.fromDocument(nullDocument, Collections.singleton("id"));
        assertNull(nullRow.getColumn(0).getValue());
    }

    @Test
    public void scanTraversesAllConfiguredPages() throws Exception {
        String previousPageSize = System.getProperty("dsp.storage.scan-page-size");
        System.setProperty("dsp.storage.scan-page-size", "2");
        File index = temporaryFolder.newFolder("paged-index");
        List<ColumnInfo> columns = columns();
        StorageEngine engine = EngineRegistry.create("lucene",
                new EngineConfig(index.getAbsolutePath(), columns,
                        Collections.<String, Object>emptyMap()));
        try {
            Row[] rows = new Row[11];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = RowImpl.of(
                        new Value(i, DataTypes.INTEGER),
                        new Value("name-" + i, DataTypes.STRING),
                        new Value(i % 2 == 0, DataTypes.BOOLEAN));
            }
            engine.append(rows);

            List<Row> scanned = scanAll(engine, columns);
            assertEquals(11, scanned.size());
            assertEquals(11, scanned.stream()
                    .map(row -> row.getColumn(0).intValue())
                    .distinct()
                    .count());
        } finally {
            engine.close();
            if (previousPageSize == null) {
                System.clearProperty("dsp.storage.scan-page-size");
            } else {
                System.setProperty("dsp.storage.scan-page-size", previousPageSize);
            }
        }
    }

    private static List<Row> scanAll(StorageEngine engine, List<ColumnInfo> columns)
            throws Exception {
        Set<String> names = new java.util.LinkedHashSet<>();
        for (ColumnInfo column : columns) {
            names.add(column.getName());
        }
        QueryContext context = new QueryContext(
                BaseQuery.builder().columnNames(names).build(), names);
        Iterator<Row> iterator = engine.scan(context);
        List<Row> rows = new ArrayList<>();
        iterator.forEachRemaining(rows::add);
        return rows;
    }

    private static List<ColumnInfo> columns() {
        return Arrays.asList(
                new ColumnInfo("id", DataTypes.INTEGER, true, null, ""),
                new ColumnInfo("name", DataTypes.STRING, true, null, ""),
                new ColumnInfo("active", DataTypes.BOOLEAN, true, null, ""));
    }

    private static MetadataStore metadata(final TableInfo table) {
        return new MetadataStore() {
            @Override
            public void createSchema(String schemaName) {
            }

            @Override
            public void dropSchema(String schemaName) {
            }

            @Override
            public List<String> listSchemas() {
                return Collections.singletonList(table.getSchema());
            }

            @Override
            public void createTable(CreateTableRequest request) {
            }

            @Override
            public void dropTable(String schema, String tableName) {
            }

            @Override
            public TableInfo getTable(String schema, String tableName) {
                return table.getSchema().equals(schema) && table.getName().equals(tableName)
                        ? table : null;
            }

            @Override
            public List<TableInfo> listTables(String schema) {
                return Collections.singletonList(table);
            }
        };
    }
}
