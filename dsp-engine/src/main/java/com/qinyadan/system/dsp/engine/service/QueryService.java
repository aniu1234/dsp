package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.calcite.ParserFactory;
import com.qinyadan.system.dsp.engine.calcite.SlothParser;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;

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

    public RelNode plan(String sql, String database, SqlNode sqlNode) {
        return parser(sql, database).getPlan(sqlNode);
    }

    private SlothParser parser(String sql, String database) {
        return ParserFactory.getParser(sql, database);
    }
}
