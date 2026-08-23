package com.qinyadan.system.dsp.schema.file;

import com.qinyadan.system.dsp.schema.common.spi.ConnectorHealth;
import com.qinyadan.system.dsp.schema.common.spi.SchemaConnectorRegistry;
import com.qinyadan.system.dsp.schema.file.csv.CsvFileSchemaFactory;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FileSchemaTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoversTablesOnceInDeterministicOrder() throws Exception {
        createTable("z_table");
        createTable("a_table");

        FileSchema schema = new FileSchema(
                temporaryFolder.getRoot().toPath(), TableTypeEnum.CSV);

        assertEquals("[a_table, z_table]", schema.getTableMap().keySet().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSchemaWithoutMatchingDataFile() throws Exception {
        File schema = temporaryFolder.newFile("orphan.schema");
        Files.write(schema.toPath(), "ID:Long\n".getBytes(StandardCharsets.UTF_8));
        new FileSchema(temporaryFolder.getRoot().toPath(), TableTypeEnum.CSV);
    }

    @Test
    public void discoversFileConnectorsThroughServiceLoader() {
        SchemaConnectorRegistry registry = SchemaConnectorRegistry.load(
                getClass().getClassLoader());

        assertTrue(registry.ids().contains("file-csv"));
        assertTrue(registry.ids().contains("file-json"));
    }

    @Test
    public void reportsHealthyFileConnector() throws Exception {
        createTable("orders");
        Map<String, Object> operand = new HashMap<>();
        operand.put("baseDirectory", temporaryFolder.getRoot());
        operand.put("directory", ".");

        ConnectorHealth health = new CsvFileSchemaFactory().health(operand);

        assertTrue(health.toString(), health.isReady());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDirectoryOutsideConfiguredBase() {
        Map<String, Object> operand = new HashMap<>();
        operand.put("baseDirectory", temporaryFolder.getRoot());
        operand.put("directory", "../outside");
        new CsvFileSchemaFactory().create(null, "csv", operand);
    }

    private void createTable(String name) throws Exception {
        File schema = temporaryFolder.newFile(name + ".schema");
        File data = temporaryFolder.newFile(name + ".csv");
        Files.write(schema.toPath(), "ID:Long\n".getBytes(StandardCharsets.UTF_8));
        Files.write(data.toPath(), "1\n".getBytes(StandardCharsets.UTF_8));
    }
}
