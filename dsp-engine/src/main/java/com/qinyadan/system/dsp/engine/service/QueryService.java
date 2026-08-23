package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.calcite.ParserFactory;
import com.qinyadan.system.dsp.engine.calcite.SlothParser;
import com.qinyadan.system.dsp.engine.rel.SlothRel;
import com.qinyadan.system.dsp.engine.service.dto.QueryColumn;
import com.qinyadan.system.dsp.engine.service.dto.QueryExecution;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL parsing and planning facade used by transport adapters.
 */
public final class QueryService {

    public static final QueryService INSTANCE = new QueryService();

    private QueryService() {
    }

    public SqlNode parse(String sql, String database) throws Exception {
        return parser(sql, database).getSqlNode();
    }

    public QueryExecution prepare(String sql, String database, SqlNode sqlNode) {
        RelNode plan = plan(sql, database, sqlNode);
        List<QueryColumn> columns = new ArrayList<>(plan.getRowType().getFieldCount());
        plan.getRowType().getFieldList().forEach(field -> columns.add(
                new QueryColumn(field.getName(),
                        field.getType().getSqlTypeName().getName())));
        return new QueryExecution(((SlothRel) plan).implement(), columns);
    }

    public String explain(String sql, String database, SqlNode sqlNode) {
        return System.lineSeparator() + RelOptUtil.toString(
                plan(sql, database, sqlNode), SqlExplainLevel.ALL_ATTRIBUTES);
    }

    private RelNode plan(String sql, String database, SqlNode sqlNode) {
        return parser(sql, database).getPlan(sqlNode);
    }

    private SlothParser parser(String sql, String database) {
        return ParserFactory.getParser(sql, database);
    }
}
