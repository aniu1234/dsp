package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WriteTableTest {

    @Test
    public void resolvesColumnsCaseInsensitivelyAndKeepsDeclaredOrder() {
        WriteTable table = new WriteTable("app", "users", Arrays.asList(
                new WriteColumn("ID", DataTypes.INTEGER, false, true, 0, null),
                new WriteColumn("display_name", DataTypes.STRING, true, false, 32, "guest")));

        assertEquals(Arrays.asList("ID", "display_name"), table.getColumnNames());
        assertNotNull(table.getColumn("id"));
        assertEquals("display_name", table.getColumn("DISPLAY_NAME").getName());
        assertEquals(32, table.getColumn("display_name").getPrecision());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void columnListIsImmutable() {
        WriteTable table = new WriteTable("app", "users", Arrays.asList(
                new WriteColumn("id", DataTypes.INTEGER, false, false, 0, null)));

        table.getColumns().clear();
    }

    @Test
    public void writeRequestDefensivelyCopiesRowsAndPreservesNullInput() {
        List<Value> sourceRow = new ArrayList<>(Collections.singletonList(
                new Value(1, DataTypes.INTEGER)));
        List<List<Value>> sourceRows = new ArrayList<>(Collections.singletonList(sourceRow));
        WriteRequest request = new WriteRequest("app", "users", sourceRows, "request-1");

        sourceRow.clear();
        sourceRows.clear();

        assertEquals(1, request.getRows().size());
        assertEquals(1, request.getRows().get(0).size());
        assertEquals("request-1", request.getIdempotencyKey());
        assertNull(new WriteRequest("app", "users", null, null).getRows());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void writeRequestRowsAreImmutable() {
        WriteRequest request = new WriteRequest("app", "users",
                Collections.singletonList(Collections.singletonList(
                        new Value(1, DataTypes.INTEGER))), null);

        request.getRows().get(0).clear();
    }
}
