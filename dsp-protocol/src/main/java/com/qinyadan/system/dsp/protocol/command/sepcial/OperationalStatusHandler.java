package com.qinyadan.system.dsp.protocol.command.sepcial;

import com.qinyadan.system.dsp.constant.ColumnTypeConstants;
import com.qinyadan.system.dsp.engine.service.HealthService;
import com.qinyadan.system.dsp.engine.service.RuntimeMetrics;
import com.qinyadan.system.dsp.engine.service.dto.HealthSnapshot;
import com.qinyadan.system.dsp.engine.service.dto.MetricsSnapshot;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SQL-visible P1 operational endpoints: SHOW DSP HEALTH / METRICS.
 */
public final class OperationalStatusHandler extends BaseHandler {

    public static final OperationalStatusHandler INSTANCE = new OperationalStatusHandler();

    private OperationalStatusHandler() {
    }

    @Override
    public void handle(ConnectionContext context, String command) {
        String normalized = command.trim().replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
        if ("SHOW DSP HEALTH".equals(normalized)) {
            writeHealth(context);
        } else {
            writeMetrics(context);
        }
    }

    private void writeHealth(ConnectionContext context) {
        HealthSnapshot health = HealthService.INSTANCE.snapshot();
        String[] columns = {"status", "detail", "databases", "tables",
                "storage_engines", "read_only_engines", "uptime_ms"};
        List<String> row = Arrays.asList(health.getStatus(), health.getDetail(),
                Integer.toString(health.getDatabases()), Integer.toString(health.getTables()),
                Integer.toString(health.getStorageEngines()),
                Integer.toString(health.getReadOnlyStorageEngines()),
                Long.toString(health.getUptimeMillis()));
        write(context, columns, Collections.singletonList(row));
    }

    private void writeMetrics(ConnectionContext context) {
        MetricsSnapshot metrics = RuntimeMetrics.INSTANCE.snapshot();
        List<List<String>> rows = new ArrayList<>();
        rows.add(metric("uptime_ms", metrics.getUptimeMillis()));
        rows.add(metric("queries_succeeded", metrics.getQueriesSucceeded()));
        rows.add(metric("queries_failed", metrics.getQueriesFailed()));
        rows.add(metric("rows_returned", metrics.getRowsReturned()));
        rows.add(metric("writes_succeeded", metrics.getWritesSucceeded()));
        rows.add(metric("writes_failed", metrics.getWritesFailed()));
        rows.add(metric("rows_written", metrics.getRowsWritten()));
        rows.add(metric("idempotent_replays", metrics.getIdempotentReplays()));
        rows.add(metric("mutations_succeeded", metrics.getMutationsSucceeded()));
        rows.add(metric("mutations_failed", metrics.getMutationsFailed()));
        rows.add(metric("rows_mutated", metrics.getRowsMutated()));
        write(context, new String[]{"metric", "value"}, rows);
    }

    private List<String> metric(String name, long value) {
        return Arrays.asList(name, Long.toString(value));
    }

    private void write(ConnectionContext context, String[] columns,
                       List<List<String>> rows) {
        List<Integer> types = new ArrayList<>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            types.add(ColumnTypeConstants.MYSQL_TYPE_VAR_STRING);
        }
        ResultSetHolder result = ResultSetHolder.builder()
                .columnName(columns)
                .columnType(types)
                .data(rows)
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();
        context.write(PackageUtils.buildResultSet(result));
    }
}
