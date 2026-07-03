package com.qinyadan.system.dsp.protocol.command.inter;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.protocol.command.AbstractCommandHandler;
import com.qinyadan.system.dsp.protocol.command.sqlnode.Handler;
import com.qinyadan.system.dsp.protocol.command.sqlnode.HandlerHolder;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.storage.parser.ParserFactory;
import com.qinyadan.system.dsp.storage.parser.SlothParser;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

import static com.qinyadan.system.dsp.constant.ColumnTypeConstants.MYSQL_TYPE_VAR_STRING;
import static com.qinyadan.system.dsp.protocol.command.sepcial.RawHandlerHolder.getRawHandler;


@Slf4j
public class QueryCommandHandler extends AbstractCommandHandler {

    private final String query;

    public QueryCommandHandler(ConnectionContext connectionContext, String query) {
        super(connectionContext);
        this.query = query;
    }

    @Override
    public void execute() {
        SqlNode sqlNode;
        try {
            final SlothParser slothParser = ParserFactory.getParser(query, connectionContext.getDb());
            connectionContext.setQueryString(query);

            //first use raw, for example 'explain'
            Handler handler = getRawHandler(query);
            if (Objects.nonNull(handler)) {
                handler.handle(connectionContext, query);
                return;
            }

            //then parser and use sqlnode
            sqlNode = slothParser.getSqlNode();
            handleSqlNode(sqlNode);
        } catch (Exception e) {

            if (query.contains("@@")) {
                handleSqlString(query);
                return;
            }

            if ("SET NAMES utf8mb4".equals(query)) {
                //
                handleSqlString(query);
                return;
            }
            //
            log.error("execute sql \n '{}' \n get error:{} \n", query, Throwables.getStackTraceAsString(e));
            connectionContext.write(PackageUtils.buildSyntaxErrPackage(e.getMessage()));
        }
    }


    private void handleSqlString(String command) {
        defaultStringHandler(command);
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
            connectionContext.write(PackageUtils.buildErrPackage(-1, e.toString(), 1));
        }
    }

    private void defaultStringHandler(String command) {
        final List<List<String>> data = Lists.newArrayListWithCapacity(1);
        data.add(Lists.newArrayList("dsp 1.0.0"));

        final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                .columnName(new String[]{command})
                .columnType(Lists.newArrayList(MYSQL_TYPE_VAR_STRING))
                .data(data)
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();

        final ByteBuf byteBuf = PackageUtils.buildResultSet(resultSetHolder);
        connectionContext.write(byteBuf);
    }
}
