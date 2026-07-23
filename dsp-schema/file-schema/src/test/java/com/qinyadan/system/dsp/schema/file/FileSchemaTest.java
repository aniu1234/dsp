package com.qinyadan.system.dsp.schema.file;

import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;


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

    private void createTable(String name) throws Exception {
        File schema = temporaryFolder.newFile(name + ".schema");
        File data = temporaryFolder.newFile(name + ".csv");
        Files.write(schema.toPath(), "ID:Long\n".getBytes(StandardCharsets.UTF_8));
        Files.write(data.toPath(), "1\n".getBytes(StandardCharsets.UTF_8));
    }
}
