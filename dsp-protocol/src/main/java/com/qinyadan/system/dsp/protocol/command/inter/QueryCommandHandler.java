package com.qinyadan.system.dsp.protocol.command.inter;

import com.google.common.base.Throwables;
import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.command.sqlnode.Handler;
import com.qinyadan.system.dsp.protocol.command.sqlnode.HandlerHolder;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.engine.service.EnvironmentService;
import com.qinyadan.system.dsp.engine.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.INTERNAL_ERROR;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.UNSUPPORTED_FEATURE;
import static com.qinyadan.system.dsp.protocol.command.sepcial.RawHandlerHolder.getRawHandler;


@Slf4j
public class QueryCommandHandler extends AbstractCommandHandler {

    private static final Pattern ENVIRONMENT_REFERENCE = Pattern.compile(
            "@@(?:(?:session|global|local)\\.)?([a-zA-Z0-9_]+)",
            Pattern.CASE_INSENSITIVE);

    private final String query;

    public QueryCommandHandler(ConnectionContext connectionContext, String query) {
        super(connectionContext);
        this.query = query;
    }

    @Override
    public void execute() {
        connectionContext.setQueryString(query);
        SqlNode sqlNode;
        try {
            if (handleCompatibilityCommand()) {
                return;
            }
            //first use raw, for example 'explain'
            Handler handler = getRawHandler(query);
            if (Objects.nonNull(handler)) {
                handler.handle(connectionContext, query);
                return;
            }

            //then parser and use sqlnode
            sqlNode = QueryService.INSTANCE.parse(
                    replaceEnvironmentReferences(query), connectionContext.getDb());
            handleSqlNode(sqlNode);
        } catch (UnsupportedOperationException e) {
            log.warn("Unsupported SQL compatibility feature in '{}': {}", query, e.getMessage());
            connectionContext.write(PackageUtils.buildErrPackage(
                    UNSUPPORTED_FEATURE.getCode(),
                    String.format(UNSUPPORTED_FEATURE.getMessage(), e.getMessage())));
        } catch (Exception e) {
            log.error("execute sql \n '{}' \n get error:{} \n", query, Throwables.getStackTraceAsString(e));
            connectionContext.write(PackageUtils.buildSyntaxErrPackage(e.getMessage()));
        } finally {
            connectionContext.setQueryString(null);
        }
    }

    private boolean handleCompatibilityCommand() {
        String normalized = query == null ? "" : query.trim()
                .replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (normalized.equals("BEGIN") || normalized.equals("COMMIT")
                || normalized.equals("ROLLBACK") || normalized.startsWith("START TRANSACTION")) {
            connectionContext.write(PackageUtils.buildErrPackage(
                    UNSUPPORTED_FEATURE.getCode(),
                    String.format(UNSUPPORTED_FEATURE.getMessage(), "transactions")));
            return true;
        }
        if (normalized.startsWith("SET NAMES ")) {
            String charset = normalized.substring("SET NAMES ".length()).trim()
                    .toLowerCase(Locale.ROOT);
            if (!charset.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("Invalid character set: " + charset);
            }
            connectionContext.getProperties().put("character_set_client", charset);
            connectionContext.getProperties().put("character_set_connection", charset);
            connectionContext.getProperties().put("character_set_results", charset);
            connectionContext.write(PackageUtils.buildOkMySqlPackage(0, 1, 0));
            return true;
        }
        return false;
    }

    /**
     * Calcite's parser does not recognize MySQL's @@variable syntax. Resolve only
     * variables declared by this server before parsing; an unknown variable must
     * remain an explicit error instead of being silently fabricated.
     */
    private String replaceEnvironmentReferences(String sql) {
        StringBuilder rewritten = new StringBuilder(sql.length());
        char quote = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : 0;

            if (lineComment) {
                rewritten.append(current);
                lineComment = current != '\n' && current != '\r';
                index++;
                continue;
            }
            if (blockComment) {
                rewritten.append(current);
                if (current == '*' && next == '/') {
                    rewritten.append(next);
                    index += 2;
                    blockComment = false;
                } else {
                    index++;
                }
                continue;
            }
            if (quote != 0) {
                rewritten.append(current);
                if (current == '\\' && next != 0) {
                    rewritten.append(next);
                    index += 2;
                } else if (current == quote && next == quote) {
                    rewritten.append(next);
                    index += 2;
                } else {
                    if (current == quote) {
                        quote = 0;
                    }
                    index++;
                }
                continue;
            }

            if (current == '/' && next == '*') {
                rewritten.append(current).append(next);
                index += 2;
                blockComment = true;
            } else if ((current == '-' && next == '-') || current == '#') {
                rewritten.append(current);
                if (next == '-') {
                    rewritten.append(next);
                    index += 2;
                } else {
                    index++;
                }
                lineComment = true;
            } else if (current == '\'' || current == '"' || current == '`') {
                rewritten.append(current);
                quote = current;
                index++;
            } else if (current == '@' && next == '@') {
                Matcher matcher = ENVIRONMENT_REFERENCE.matcher(sql);
                matcher.region(index, sql.length());
                if (matcher.lookingAt()) {
                    rewritten.append(environmentLiteral(matcher.group(1)));
                    index = matcher.end();
                } else {
                    rewritten.append(current);
                    index++;
                }
            } else {
                rewritten.append(current);
                index++;
            }
        }
        return rewritten.toString();
    }

    private String environmentLiteral(String variable) {
        String key = variable.toLowerCase(Locale.ROOT);
        String value = connectionContext.getProperties().get(key);
        if (value == null) {
            value = EnvironmentService.INSTANCE.getGlobal(key);
        }
        if (value == null) {
            throw new UnsupportedOperationException("environment variable '@@" + key + "'");
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private void handleSqlNode(SqlNode sqlNode) {
        final Class<?> sqlType = sqlNode.getClass();
        final Handler handler = HandlerHolder.SQL_TYPE_TO_HANDLER_MAP.get(sqlType);
        if (Objects.isNull(handler)) {
            //
            final MysqlPackage result = PackageUtils.buildErrPackage(1, "Do not support: " + sqlType, 1);
            connectionContext.getChannelHandlerContext().writeAndFlush(PackageUtils.packageToBuf(result));
            return;
        }

        try {
            handler.handle(connectionContext, sqlNode);
        } catch (Throwable e) {
            log.error("Execute sql '{}' get error: {}",
                    connectionContext.getQueryString(),
                    Throwables.getStackTraceAsString(e)
            );
            connectionContext.write(PackageUtils.buildErrPackage(
                    INTERNAL_ERROR.getCode(),
                    String.format(INTERNAL_ERROR.getMessage(), e.getMessage()),
                    1));
        }
    }

}
