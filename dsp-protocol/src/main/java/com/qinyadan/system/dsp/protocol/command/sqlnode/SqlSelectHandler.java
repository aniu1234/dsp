package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.qinyadan.system.dsp.constant.ColumnTypeConstants;
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
import com.qinyadan.system.dsp.engine.calcite.ParserFactory;
import com.qinyadan.system.dsp.engine.calcite.SlothParser;
import com.qinyadan.system.dsp.engine.rel.SlothRel;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Collections;
import java.util.Objects;
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

        SlothParser slothParser = ParserFactory.getParser(connectionContext.getQueryString(),
                connectionContext.getDb());
        type = type.accept(new EnvironmentReplaceVisitor(connectionContext));
        final RelNode relNode = slothParser.getPlan(type);

        final Operator<SlothRow> operator = ((SlothRel) relNode).implement();

        final List<String> columnNames = relNode.getRowType().getFieldNames();
        final List<Integer> rowTypes = relNode.getRowType()
                .getFieldList()
                .stream()
                .map(f -> f.getType().getSqlTypeName())
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
        long deadline = timeoutMillis == 0 ? Long.MAX_VALUE
                : System.nanoTime() + timeoutMillis * 1000000L;
        int maximumRows = maxResultRows();
        int rowCount = 0;
        try {
            openAttempted = true;
            operator.open();
            checkDeadline(deadline, timeoutMillis);
            writer.start(metadata);
            SlothRow tmp;
            while (true) {
                checkDeadline(deadline, timeoutMillis);
                tmp = operator.next();
                checkDeadline(deadline, timeoutMillis);
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
                writer.finish();
            } else {
                writer.fail(errorFor(failure));
            }
        } finally {
            writer.close();
        }
    }

    private void checkDeadline(long deadline, long timeoutMillis) {
        if (System.nanoTime() > deadline) {
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
        return positiveConfiguration("dsp.query.max-result-rows",
                "DSP_QUERY_MAX_RESULT_ROWS", 100000, false);
    }

    private long queryTimeoutMillis() {
        return positiveConfiguration("dsp.query.timeout-ms",
                "DSP_QUERY_TIMEOUT_MS", 30000, true);
    }

    private int positiveConfiguration(String property, String environment,
                                      int defaultValue, boolean zeroAllowed) {
        String configured = System.getProperty(property);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(environment);
        }
        if (configured == null || configured.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(configured);
            if (value < 0 || (!zeroAllowed && value == 0)) {
                throw new IllegalArgumentException("Invalid query limit: " + configured);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid query limit: " + configured, e);
        }
    }

    private static final class QueryTimeoutException extends RuntimeException {
        private final long timeoutMillis;

        private QueryTimeoutException(long timeoutMillis) {
            super("Query exceeded " + timeoutMillis + " ms");
            this.timeoutMillis = timeoutMillis;
        }
    }
}
