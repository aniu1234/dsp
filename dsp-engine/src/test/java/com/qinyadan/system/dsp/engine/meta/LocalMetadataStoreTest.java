package com.qinyadan.system.dsp.engine.meta;

import com.qinyadan.system.dsp.engine.calcite.EnhanceSlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothColumn;
import com.qinyadan.system.dsp.engine.calcite.SlothSchema;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalMetadataStoreTest {

    @Test
    public void persistsAndRestoresCatalogWithoutExternalMysql() throws Exception {
        Path dataDirectory = Files.createTempDirectory("dsp-local-catalog");
        String previousDataDirectory = System.getProperty("dsp.data.dir");
        System.setProperty("dsp.data.dir", dataDirectory.toString());
        try {
            Path catalog = dataDirectory.resolve("_meta").resolve(LocalMetadataStore.CATALOG_FILE);
            LocalMetadataStore writer = new LocalMetadataStore(catalog);
            writer.addSchema("sales");

            SlothSchema schema = new SlothSchema("sales");
            SlothTable table = new SlothTable("orders");
            table.setSchema(schema);
            table.setEngineName("lucene");
            table.setShardNum(2);
            table.setTableComment("orders table");
            table.setColumns(Arrays.asList(
                    column("id", SqlTypeName.INTEGER, false, null),
                    column("name", SqlTypeName.VARCHAR, true, "guest")));
            writer.addTable("sales", table);

            LocalMetadataStore reader = new LocalMetadataStore(catalog);
            assertTrue(reader.allSchemas().contains("sales"));
            List<SlothTable> tables = reader.getAllTables(schema);
            assertEquals(1, tables.size());
            SlothTable restored = tables.get(0);
            assertEquals("orders", restored.getTableName());
            assertEquals("lucene", restored.getEngineName());
            assertEquals(2, restored.getShardNum());
            assertEquals("guest", restored.getColumns().get(1)
                    .getColumnType().getDefalutValue());
        } finally {
            if (previousDataDirectory == null) {
                System.clearProperty("dsp.data.dir");
            } else {
                System.setProperty("dsp.data.dir", previousDataDirectory);
            }
            FileUtils.deleteDirectory(dataDirectory.toFile());
        }
    }

    private static SlothColumn column(String name, SqlTypeName type,
                                      boolean nullable, String defaultValue) {
        EnhanceSlothColumn definition = new EnhanceSlothColumn();
        definition.setColumName(name);
        definition.setColumnType(type);
        definition.setNullable(nullable);
        definition.setDefalutValue(defaultValue);
        return new SlothColumn(name, definition);
    }
}
