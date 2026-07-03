package com.qinyadan.system.dsp.protocol.command.sepcial;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.constant.StringConstants;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.protocol.visitor.EnvironmentReplaceVisitor;
import com.qinyadan.system.dsp.storage.parser.ParserFactory;
import com.qinyadan.system.dsp.storage.parser.SlothParser;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.qinyadan.system.dsp.constant.ColumnTypeConstants.MYSQL_TYPE_VAR_STRING;


@Slf4j
public class ExplainHandler extends BaseHandler {

    public static final ExplainHandler INSTANCE = new ExplainHandler();

    private static final String EXPLAIN_RESULT_COLUMN = "Plan";

    @Override
    public void handle(ConnectionContext connectionContext, String type) {

        final String query = type.split(StringConstants.SPACE, 2)[1];
        final SlothParser slothParser = ParserFactory.getParser(query, connectionContext.getDb());

        ByteBuf result;
        try {
            final SqlNode sqlNode = slothParser.getSqlNode(query);
            final RelNode relNode = slothParser.getPlan(sqlNode.accept(new EnvironmentReplaceVisitor(connectionContext)));
            final String planString = StringConstants.LINE_SEPARATOR + RelOptUtil.toString(relNode, SqlExplainLevel.ALL_ATTRIBUTES);
            final List<List<String>> data = Lists.newArrayList();
            data.add(Lists.newArrayList(planString));

            final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                    .table(StringUtils.EMPTY)
                    .columnType(Lists.newArrayList(MYSQL_TYPE_VAR_STRING))
                    .schema(StringUtils.EMPTY)
                    .data(data)
                    .columnName(new String[]{EXPLAIN_RESULT_COLUMN})
                    .build();

            result = PackageUtils.buildResultSet(resultSetHolder);
        } catch (Throwable e) {
            log.error(Throwables.getStackTraceAsString(e));
            result = PackageUtils.packageToBuf(PackageUtils.buildSyntaxErrPackage(query));
        }

        connectionContext.write(result);
    }
}
