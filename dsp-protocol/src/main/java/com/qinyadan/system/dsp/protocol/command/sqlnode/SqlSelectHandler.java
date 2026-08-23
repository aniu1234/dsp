package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.constant.ColumnTypeConstants;
import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.protocol.command.sepcial.SpecialSelectHolder;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetStreamWriter;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.protocol.visitor.EnvironmentReplaceVisitor;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.operator.Operator;
import com.qinyadan.system.dsp.engine.operator.QueryResourceLimitException;
import com.qinyadan.system.dsp.engine.service.QueryService;
import com.qinyadan.system.dsp.engine.service.RuntimeMetrics;
import com.qinyadan.system.dsp.engine.service.dto.QueryExecution;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.INTERNAL_ERROR;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.QUERY_RESOURCE_LIMIT;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.QUERY_TIMEOUT;


public class SqlSelectHandler implements Handler<SqlNode> {

    public static final SqlSelectHandler INSTANCE = new SqlSelectHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlNode type) {

        String s = type.toString();
        Handler handler;
        if ((handler = SpecialSelectHolder.SPECIAL_HANDLER.get(s)) != null) {
            handler.handle(connectionContext, s);
            return;
        }

        type = type.accept(new EnvironmentReplaceVisitor(connectionContext));
        final QueryExecution execution = QueryService.INSTANCE.prepare(
                connectionContext.getQueryString(), connectionContext.getDb(), type);
        final Operator<SlothRow> operator = execution.getOperator();
        final List<String> columnNames = execution.getColumns().stream()
                .map(column -> column.getName())
                .collect(Collectors.toList());
        final List<Integer> rowTypes = execution.getColumns().stream()
                .map(column -> SqlTypeName.get(column.getSqlType()))
                .map(ColumnTypeConstants::getMysqlType)
                .collect(Collectors.toList());

        final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                .columnName(columnNames.toArray(new String[0]))
                .columnType(rowTypes)
                .data(Collections.<List<String>>emptyList())
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();

        streamOperator(connectionContext, operator, resultSetHolder);
    }

    /**
     * Execute the Mpp iterator operator
     *
     * @param operator
     * @return
     */
    private void streamOperator(ConnectionContext connectionContext, Operator<SlothRow> operator,
                                ResultSetHolder metadata) {
        ResultSetStreamWriter writer = new ResultSetStreamWriter(connectionContext);
        RuntimeException failure = null;
        boolean openAttempted = false;
        long timeoutMillis = queryTimeoutMillis();
        long startedAt = System.nanoTime();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        int maximumRows = maxResultRows();
        int rowCount = 0;
        try {
            openAttempted = true;
            operator.open();
            checkDeadline(startedAt, timeoutNanos, timeoutMillis);
            writer.start(metadata);
            SlothRow tmp;
            while (true) {
                checkDeadline(startedAt, timeoutNanos, timeoutMillis);
                tmp = operator.next();
                checkDeadline(startedAt, timeoutNanos, timeoutMillis);
                if (tmp == SlothRow.EOF_ROW) {
                    break;
                }
                rowCount++;
                if (rowCount > maximumRows) {
                    throw new QueryResourceLimitException(
                            "result exceeded the row limit of " + maximumRows);
                }
                List<String> row = tmp.getAllColumn().stream()
                        .map(Value::getValueByType)
                        .map(value -> Objects.isNull(value) ? null : value.toString())
                        .collect(Collectors.toList());
                writer.writeRow(row);
            }
        } catch (RuntimeException e) {
            failure = e;
        } finally {
            if (openAttempted) {
                try {
                    operator.close();
                } catch (RuntimeException closeError) {
                    if (failure == null) {
                        failure = closeError;
                    } else {
                        failure.addSuppressed(closeError);
                    }
                }
            }
        }
        try {
            if (failure == null) {
                try {
                    writer.finish();
                } catch (RuntimeException finishError) {
                    RuntimeMetrics.INSTANCE.queryFailed();
                    throw finishError;
                }
                RuntimeMetrics.INSTANCE.querySucceeded(rowCount);
            } else {
                RuntimeMetrics.INSTANCE.queryFailed();
                writer.fail(errorFor(failure));
            }
        } finally {
            writer.close();
        }
    }

    private void checkDeadline(long startedAt, long timeoutNanos, long timeoutMillis) {
        if (timeoutMillis > 0 && System.nanoTime() - startedAt > timeoutNanos) {
            throw new QueryTimeoutException(timeoutMillis);
        }
    }

    private MysqlPackage errorFor(RuntimeException failure) {
        if (failure instanceof QueryTimeoutException) {
            return PackageUtils.buildErrPackage(QUERY_TIMEOUT.getCode(),
                    String.format(QUERY_TIMEOUT.getMessage(),
                            ((QueryTimeoutException) failure).timeoutMillis));
        }
        if (failure instanceof QueryResourceLimitException) {
            return PackageUtils.buildErrPackage(QUERY_RESOURCE_LIMIT.getCode(),
                    String.format(QUERY_RESOURCE_LIMIT.getMessage(), failure.getMessage()));
        }
        return PackageUtils.buildErrPackage(INTERNAL_ERROR.getCode(),
                String.format(INTERNAL_ERROR.getMessage(), failure.getMessage()));
    }

    private int maxResultRows() {
        return DspConfiguration.load().getQueryMaxResultRows();
    }

    private long queryTimeoutMillis() {
        return DspConfiguration.load().getQueryTimeoutMillis();
    }

    private static final class QueryTimeoutException extends RuntimeException {
        private final long timeoutMillis;

        private QueryTimeoutException(long timeoutMillis) {
            super("Query exceeded " + timeoutMillis + " ms");
            this.timeoutMillis = timeoutMillis;
        }
    }
}
