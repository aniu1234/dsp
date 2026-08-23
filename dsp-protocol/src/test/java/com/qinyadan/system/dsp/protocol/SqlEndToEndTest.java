package com.qinyadan.system.dsp.protocol;

import com.qinyadan.system.dsp.engine.service.CatalogService;
import com.qinyadan.system.dsp.engine.service.EnvironmentService;
import com.qinyadan.system.dsp.protocol.command.inter.QueryCommandHandler;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlEndToEndTest {

    private static Path dataDirectory;
    private static EmbeddedChannel channel;
    private static ConnectionContext connectionContext;

    @BeforeClass
    public static void setUp() throws Exception {
        dataDirectory = Files.createTempDirectory("dsp-sql-e2e");
        System.setProperty("dsp.data.dir", dataDirectory.toString());
        EnvironmentService.INSTANCE.init();
        CatalogService.INSTANCE.init();

        ContextCapture capture = new ContextCapture();
        channel = new EmbeddedChannel(capture);
        connectionContext = new ConnectionContext(capture.context);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (CatalogService.INSTANCE.databaseExists("p0_test")) {
            execute("DROP DATABASE p0_test");
        }
        CatalogService.INSTANCE.close();
        EnvironmentService.INSTANCE.close();
        channel.finishAndReleaseAll();
        System.clearProperty("dsp.data.dir");
        FileUtils.deleteDirectory(dataDirectory.toFile());
    }

    @Test
    public void completeSqlPathEnforcesDefaultsNullsUnionAndJoin() {
        assertOk(execute("CREATE DATABASE p0_test"));
        assertOk(execute("USE p0_test"));
        assertOk(execute("CREATE TABLE users (id INTEGER NOT NULL, name VARCHAR DEFAULT 'guest') ENGINE = lucene"));
        assertOk(execute("CREATE TABLE teams (id INTEGER NOT NULL, label VARCHAR NOT NULL) ENGINE = lucene"));
        assertOk(execute("CREATE TABLE events (id INTEGER NOT NULL, event_day DATE, "
                + "created_at TIMESTAMP) ENGINE = lucene"));
        assertOk(execute("CREATE TABLE key_left (a INTEGER, b INTEGER, name VARCHAR) "
                + "ENGINE = lucene"));
        assertOk(execute("CREATE TABLE key_right (a INTEGER, b INTEGER, label VARCHAR) "
                + "ENGINE = lucene"));
        assertOk(execute("CREATE TABLE agg_values (group_name VARCHAR, amount INTEGER) "
                + "ENGINE = lucene"));
        assertOk(execute("CREATE TABLE write_rules (id INTEGER UNSIGNED NOT NULL, "
                + "code VARCHAR(3) NOT NULL) ENGINE = lucene"));

        assertOk(execute("INSERT INTO users(id) VALUES (1)"));
        assertOk(execute("INSERT INTO users(id, name) VALUES (2, 'member'), (3, NULL)"));
        assertOk(execute("INSERT INTO users(id, name) VALUES (4, '中文')"));
        assertOk(execute("INSERT INTO teams(id, label) VALUES (2, 'staff')"));
        assertOk(execute("INSERT INTO events(id, event_day, created_at) "
                + "VALUES (1, '2026-07-20', '2026-07-20 14:30:45')"));
        assertOk(execute("INSERT INTO key_left(a, b, name) VALUES "
                + "(1, 1, 'l11'), (1, 2, 'l12'), (NULL, 2, 'ln')"));
        assertOk(execute("INSERT INTO key_right(a, b, label) VALUES "
                + "(1, 1, 'r11'), (1, 1, 'r11b'), (1, 3, 'r13'), (NULL, 2, 'rn')"));
        assertOk(execute("INSERT INTO agg_values(group_name, amount) VALUES "
                + "('a', 1), ('a', 2), ('a', NULL), ('b', 5), (NULL, 7)"));
        assertEquals(Arrays.asList(Arrays.asList("1", "guest")),
                resultRows(execute("SELECT id, name FROM users WHERE id = 1"), 2));
        String previousMaterializedLimit =
                System.getProperty("dsp.query.max-materialized-rows");
        System.setProperty("dsp.query.max-materialized-rows", "1");
        try {
            // The four-row users side must be streamed; only the one-row teams side is built.
            assertEquals(Arrays.asList(Arrays.asList("2", "member", "staff")),
                    resultRows(execute("SELECT u.id, u.name, g.label FROM users u "
                            + "JOIN teams g ON u.id = g.id"), 3));
            // Non-equi predicates use the bounded, one-side-materialized fallback.
            assertEquals(Arrays.asList(Arrays.asList("3"), Arrays.asList("4")),
                    resultRows(execute("SELECT u.id FROM users u JOIN teams g "
                            + "ON u.id > g.id"), 1));
            // Global non-distinct aggregation keeps one state despite four input rows.
            assertEquals(Arrays.asList(Arrays.asList("4", "10")),
                    resultRows(execute("SELECT COUNT(*), SUM(id) FROM users"), 2));
        } finally {
            restoreProperty("dsp.query.max-materialized-rows", previousMaterializedLimit);
        }

        assertEquals(Arrays.asList(
                        Arrays.asList("1", "1", "l11", "r11"),
                        Arrays.asList("1", "1", "l11", "r11b")),
                resultRows(execute("SELECT l.a, l.b, l.name, r.label FROM key_left l "
                        + "JOIN key_right r ON l.a = r.a AND l.b = r.b ORDER BY r.label"), 4));
        assertEquals(Arrays.asList(Arrays.asList("l11", "r11b")),
                resultRows(execute("SELECT l.name, r.label FROM key_left l JOIN key_right r "
                        + "ON l.a = r.a AND l.b = r.b AND r.label = 'r11b'"), 2));
        assertEquals(Arrays.asList(
                        Arrays.asList("l11", "r11"),
                        Arrays.asList("l11", "r11b"),
                        Arrays.asList("l12", null),
                        Arrays.asList("ln", null)),
                resultRows(execute("SELECT l.name, r.label FROM key_left l LEFT JOIN "
                        + "key_right r ON l.a = r.a AND l.b = r.b "
                        + "ORDER BY l.name, r.label"), 2));
        assertEquals(Arrays.asList(
                        Arrays.asList("l11", "r11"),
                        Arrays.asList("l11", "r11b"),
                        Arrays.asList(null, "r13"),
                        Arrays.asList(null, "rn")),
                resultRows(execute("SELECT l.name, r.label FROM key_left l RIGHT JOIN "
                        + "key_right r ON l.a = r.a AND l.b = r.b ORDER BY r.label"), 2));
        assertEquals(Arrays.asList(Arrays.asList("2")),
                resultRows(execute("SELECT id FROM users WHERE name = 'member'"), 1));
        assertEquals(Arrays.asList(Arrays.asList("中文")),
                resultRows(execute("SELECT name FROM users WHERE id = 4"), 1));
        assertEquals(Arrays.asList(Arrays.asList("2026-07-20", "2026-07-20 14:30:45")),
                resultRows(execute("SELECT event_day, created_at FROM events WHERE id = 1"), 2));
        assertEquals(Arrays.asList(Arrays.asList("1"), Arrays.asList("2"),
                        Arrays.asList("3"), Arrays.asList("4")),
                resultRows(execute("SELECT id FROM users UNION SELECT id FROM users ORDER BY id"), 1));
        assertEquals(Arrays.asList(Arrays.asList("@@version")),
                resultRows(execute("SELECT '@@version'"), 1));
        assertEquals(Arrays.asList(Arrays.asList("3")),
                resultRows(execute("SELECT COUNT(DISTINCT name) FROM users"), 1));
        assertEquals(Arrays.asList(Arrays.asList("0", null)),
                resultRows(execute("SELECT COUNT(*), SUM(id) FROM users WHERE id = 999"), 2));
        assertEquals(Arrays.asList(
                        Arrays.asList(null, "1", "7", "7", "7"),
                        Arrays.asList("a", "2", "3", "1", "2"),
                        Arrays.asList("b", "1", "5", "5", "5")),
                resultRows(execute("SELECT group_name, COUNT(amount), SUM(amount), "
                        + "MIN(amount), MAX(amount) FROM agg_values GROUP BY group_name "
                        + "ORDER BY group_name"), 5));

        byte[] signedInsert = execute(
                "INSERT INTO users(`ID`, `NAME`) VALUES (-5, DEFAULT), (+6, 'signed')");
        assertOk(signedInsert);
        assertEquals(2, affectedRows(signedInsert));
        assertEquals(Arrays.asList(
                        Arrays.asList("-5", "guest"),
                        Arrays.asList("1", "guest"),
                        Arrays.asList("2", "member"),
                        Arrays.asList("3", null),
                        Arrays.asList("4", "中文"),
                        Arrays.asList("6", "signed")),
                resultRows(execute("SELECT id, name FROM users ORDER BY id"), 2));

        byte[] notNullError = execute("INSERT INTO users(id, name) VALUES (NULL, 'bad')");
        assertEquals(1048, errorCode(notNullError));
        assertEquals(1366, errorCode(execute(
                "INSERT INTO write_rules(id, code) VALUES (-1, 'ok')")));
        assertEquals(1366, errorCode(execute(
                "INSERT INTO write_rules(id, code) VALUES (1.5, 'ok')")));
        assertEquals(1366, errorCode(execute(
                "INSERT INTO write_rules(id, code) VALUES (1, 'long')")));

        // The complete VALUES list is validated before the storage batch starts.
        assertEquals(1048, errorCode(execute(
                "INSERT INTO teams(id, label) VALUES (3, 'valid'), (4, NULL)")));
        assertEquals(new ArrayList<List<String>>(),
                resultRows(execute("SELECT id FROM teams WHERE id = 3"), 1));
        assertEquals(new ArrayList<List<String>>(),
                resultRows(execute("SELECT id FROM teams WHERE id = 4"), 1));

        String previousWriteLimit = System.getProperty("dsp.write.max-rows-per-insert");
        System.setProperty("dsp.write.max-rows-per-insert", "1");
        try {
            assertEquals(1226, errorCode(execute(
                    "INSERT INTO users(id) VALUES (7), (8)")));
            assertEquals(new ArrayList<List<String>>(),
                    resultRows(execute("SELECT id FROM users WHERE id = 7"), 1));
            assertEquals(new ArrayList<List<String>>(),
                    resultRows(execute("SELECT id FROM users WHERE id = 8"), 1));
        } finally {
            restoreProperty("dsp.write.max-rows-per-insert", previousWriteLimit);
        }

        String idempotentInsert = "INSERT INTO users(id, name) VALUES (9, 'once') "
                + "/* dsp:idempotency-key=users-9 */";
        assertOk(execute(idempotentInsert));
        assertOk(execute(idempotentInsert));
        assertEquals(Arrays.asList(Arrays.asList("9", "once")),
                resultRows(execute("SELECT id, name FROM users WHERE id = 9"), 2));
        assertEquals(1062, errorCode(execute(
                "INSERT INTO users(id, name) VALUES (10, 'conflict') "
                        + "/* dsp:idempotency-key=users-9 */")));

        List<List<String>> health = resultRows(execute("SHOW DSP HEALTH"), 7);
        assertEquals("UP", health.get(0).get(0));
        List<List<String>> metrics = resultRows(execute("SHOW DSP METRICS"), 2);
        assertTrue(metrics.stream().anyMatch(row -> "idempotent_replays".equals(row.get(0))));
        assertEquals(1235, errorCode(execute("BEGIN")));
        assertEquals(1235, errorCode(execute("SELECT @@not_a_dsp_variable")));
    }

    private static byte[] execute(String sql) {
        new QueryCommandHandler(connectionContext, sql).execute();
        ByteBuf combined = Unpooled.buffer();
        ByteBuf outbound;
        while ((outbound = channel.readOutbound()) != null) {
            combined.writeBytes(outbound);
            outbound.release();
        }
        try {
            byte[] result = new byte[combined.readableBytes()];
            combined.readBytes(result);
            return result;
        } finally {
            combined.release();
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void assertOk(byte[] response) {
        List<byte[]> payloads = payloads(response);
        assertEquals(1, payloads.size());
        assertEquals(0, payloads.get(0)[0] & 0xff);
    }

    private static int errorCode(byte[] response) {
        byte[] payload = payloads(response).get(0);
        assertEquals(0xff, payload[0] & 0xff);
        return (payload[1] & 0xff) | ((payload[2] & 0xff) << 8);
    }

    private static int affectedRows(byte[] response) {
        byte[] payload = payloads(response).get(0);
        assertEquals(0, payload[0] & 0xff);
        int encoded = payload[1] & 0xff;
        if (encoded > 0xfa) {
            throw new IllegalArgumentException(
                    "Test helper only supports small affected-row counts");
        }
        return encoded;
    }

    private static List<List<String>> resultRows(byte[] response, int columnCount) {
        List<byte[]> packets = payloads(response);
        assertTrue(packets.size() >= columnCount + 3);
        assertEquals(columnCount, packets.get(0)[0] & 0xff);
        List<List<String>> rows = new ArrayList<>();
        for (int i = columnCount + 2; i < packets.size() - 1; i++) {
            rows.add(decodeRow(packets.get(i), columnCount));
        }
        return rows;
    }

    private static List<String> decodeRow(byte[] payload, int columnCount) {
        List<String> values = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < columnCount; i++) {
            int marker = payload[offset++] & 0xff;
            if (marker == 0xfb) {
                values.add(null);
            } else {
                int length;
                if (marker == 0xfc) {
                    length = (payload[offset] & 0xff) | ((payload[offset + 1] & 0xff) << 8);
                    offset += 2;
                } else if (marker == 0xfd) {
                    length = (payload[offset] & 0xff) | ((payload[offset + 1] & 0xff) << 8)
                            | ((payload[offset + 2] & 0xff) << 16);
                    offset += 3;
                } else if (marker == 0xfe) {
                    long longLength = 0;
                    for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
                        longLength |= ((long) payload[offset + byteIndex] & 0xff)
                                << (8 * byteIndex);
                    }
                    if (longLength > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Test row value is too large");
                    }
                    length = (int) longLength;
                    offset += 8;
                } else {
                    length = marker;
                }
                values.add(new String(payload, offset, length, StandardCharsets.UTF_8));
                offset += length;
            }
        }
        return values;
    }

    private static List<byte[]> payloads(byte[] response) {
        List<byte[]> result = new ArrayList<>();
        int offset = 0;
        while (offset < response.length) {
            int length = (response[offset] & 0xff)
                    | ((response[offset + 1] & 0xff) << 8)
                    | ((response[offset + 2] & 0xff) << 16);
            offset += 4;
            result.add(Arrays.copyOfRange(response, offset, offset + length));
            offset += length;
        }
        return result;
    }

    private static final class ContextCapture extends ChannelInboundHandlerAdapter {
        private ChannelHandlerContext context;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            context = ctx;
        }
    }
}
