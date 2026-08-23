package com.qinyadan.system.dsp.engine.service.dto;

import com.qinyadan.system.dsp.core.data.type.DataTypes;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
