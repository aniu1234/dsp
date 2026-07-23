package com.qinyadan.system.dsp.schema.file.json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;


public class JsonFileReaderTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsPrettyPrintedObjectsInDeclaredColumnOrder() throws Exception {
        File schema = temporaryFolder.newFile("users.schema");
        File data = temporaryFolder.newFile("users.json");
        Files.write(schema.toPath(), "ID:Long\nNAME:String\n"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(data.toPath(), ("{\n  \"name\": \"alice\",\n  \"id\": 7\n}\n"
                + "{\"id\":8,\"name\":\"bob\"}\n").getBytes(StandardCharsets.UTF_8));

        Iterator<Object[]> rows = new JsonFileReader(
                data.getPath(), schema.getPath()).readData();

        assertArrayEquals(new Object[] {7L, "alice"}, rows.next());
        assertArrayEquals(new Object[] {8L, "bob"}, rows.next());
        assertFalse(rows.hasNext());
    }
}
