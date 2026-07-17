package com.qinyadan.system.dsp.engine.lucene;

import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.data.type.DataTypes;
import com.qinyadan.system.dsp.engine.data.value.Value;
import com.qinyadan.system.dsp.storage.api.query.BaseQuery;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class LuceneStorageEngineTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousDataDirectory;

    @Before
    public void configureDataDirectory() {
        previousDataDirectory = System.getProperty("dsp.data.dir");
        System.setProperty("dsp.data.dir", temporaryFolder.getRoot().getAbsolutePath());
    }

    @After
    public void restoreDataDirectory() {
        if (previousDataDirectory == null) {
            System.clearProperty("dsp.data.dir");
        } else {
            System.setProperty("dsp.data.dir", previousDataDirectory);
        }
    }

    @Test
    public void rowsAreImmediatelyVisibleAndPersistedPerShard() {
        SlothTable table = createTable();
        table.initTableEngine();
        table.getSlothTableEngine().insert(Arrays.asList(Arrays.asList(
                new Value(7, DataTypes.INTEGER), new Value("hello", DataTypes.STRING))));

        assertEquals(Integer.valueOf(7), firstRow(table).getColumn(0).intValue());
        assertTrue(new File(table.buildTableEnginePath(), "0").isDirectory());
        assertTrue(new File(table.buildTableEnginePath(), "1").isDirectory());
        table.getSlothTableEngine().close();

        SlothTable reopened = createTable();
        reopened.initTableEngine();
        assertEquals("hello", firstRow(reopened).getColumn(1).stringValue());
        reopened.getSlothTableEngine().close();
    }

    @Test
    public void nullMarkersDoNotCollideWithRealValues() {
        SlothTable table = createTable();
        table.initTableEngine();
        table.getSlothTableEngine().insert(Arrays.asList(
                Arrays.asList(new Value(Integer.MIN_VALUE, DataTypes.INTEGER),
                        new Value("NULL", DataTypes.STRING)),
                Arrays.asList(new Value(null, DataTypes.INTEGER),
                        new Value(null, DataTypes.STRING))));

        List<SlothRow> rows = allRows(table);
        assertEquals(2, rows.size());
        SlothRow nullRow = rows.stream()
                .filter(row -> row.getColumn(0).isNull())
                .findFirst()
                .orElse(null);
        SlothRow valueRow = rows.stream()
                .filter(row -> !row.getColumn(0).isNull())
                .findFirst()
                .orElse(null);
        assertNotNull(nullRow);
        assertNotNull(valueRow);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), valueRow.getColumn(0).intValue());
        assertEquals("NULL", valueRow.getColumn(1).stringValue());
        assertNull(nullRow.getColumn(0).getValue());
        assertNull(nullRow.getColumn(1).getValue());
        table.getSlothTableEngine().close();
    }

    private static SlothRow firstRow(SlothTable table) {
        return allRows(table).get(0);
    }

    private static List<SlothRow> allRows(SlothTable table) {
        HashSet<String> columns = new HashSet<>(table.getSlothTableEngine().getColumnNames());
        QueryContext context = new QueryContext(
                BaseQuery.builder().columnNames(columns).build(), columns);
        Iterator<SlothRow> rows = table.getSlothTableEngine().search(context);
        List<SlothRow> result = new ArrayList<>();
        rows.forEachRemaining(result::add);
        return result;
    }

    private static SlothTable createTable() {
        SlothSchema schema = new SlothSchema("test_db");
        SlothTable table = new SlothTable(schema);
        table.setTableName("test_table");
        table.setShardNum(2);
        table.setColumns(Arrays.asList(
                column("id", SqlTypeName.INTEGER),
                column("name", SqlTypeName.VARCHAR)));
        return table;
    }

    private static SlothColumn column(String name, SqlTypeName type) {
        EnhanceSlothColumn column = new EnhanceSlothColumn();
        column.setColumName(name);
        column.setColumnType(type);
        column.setNullable(true);
        return new SlothColumn(name, column);
    }
}
