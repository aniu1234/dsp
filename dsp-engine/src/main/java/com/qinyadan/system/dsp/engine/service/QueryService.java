package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.engine.calcite.ParserFactory;
import com.qinyadan.system.dsp.engine.calcite.EnvironmentValueHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothParser;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;
import com.qinyadan.system.dsp.engine.rel.SlothRel;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlInsert;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.type.SqlTypeName;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Application boundary for parsing, planning and executing SQL queries.
 */
public final class QueryService {

    public static final QueryService INSTANCE = new QueryService();

    private QueryService() {
    }

    public int insert(SqlInsert insert, String currentDatabase) {
        return new InsertExecutor().execute(insert, currentDatabase);
    }

    public String environmentValue(String name) {
        return EnvironmentValueHolder.INSTACNE.propertyValue(name);
    }

    public void setEnvironmentValue(String name, String value) {
        EnvironmentValueHolder.INSTACNE.add(name, value);
    }

    public SqlNode parse(String sql, String currentDatabase) throws SqlParseException {
        return ParserFactory.getParser(sql, currentDatabase).getSqlNode();
    }

    public QueryExecution prepare(String sql, String currentDatabase, SqlNode sqlNode) {
        SlothParser parser = ParserFactory.getParser(sql, currentDatabase);
        RelNode plan = parser.getPlan(sqlNode);
        if (!(plan instanceof SlothRel)) {
            throw new IllegalStateException("Query plan is not executable: " + plan.getClass().getName());
        }

        Operator<SlothRow> operator = ((SlothRel) plan).implement();
        List<QueryColumn> columns = new ArrayList<>(plan.getRowType().getFieldCount());
        for (RelDataTypeField field : plan.getRowType().getFieldList()) {
            columns.add(new QueryColumn(field.getName(),
                    jdbcType(field.getType().getSqlTypeName())));
        }
        return new QueryExecution(columns, operator);
    }

    private int jdbcType(SqlTypeName type) {
        switch (type) {
            case BOOLEAN: return Types.BOOLEAN;
            case TINYINT: return Types.TINYINT;
            case SMALLINT: return Types.SMALLINT;
            case INTEGER: return Types.INTEGER;
            case BIGINT: return Types.BIGINT;
            case FLOAT: return Types.FLOAT;
            case REAL: return Types.REAL;
            case DOUBLE: return Types.DOUBLE;
            case DECIMAL: return Types.DECIMAL;
            case DATE: return Types.DATE;
            case TIME: return Types.TIME;
            case TIMESTAMP: return Types.TIMESTAMP;
            case CHAR: return Types.CHAR;
            case VARCHAR: return Types.VARCHAR;
            default: return Types.OTHER;
        }
    }

    public String explain(String sql, String currentDatabase, SqlNode sqlNode) {
        SlothParser parser = ParserFactory.getParser(sql, currentDatabase);
        RelNode plan = parser.getPlan(sqlNode);
        return System.lineSeparator()
                + RelOptUtil.toString(plan, SqlExplainLevel.ALL_ATTRIBUTES);
    }
}
