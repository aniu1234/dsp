package com.qinyadan.system.dsp.engine.parser.ddl;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.annotation.Nonnull;
import java.util.List;


public class SqlUse extends SqlDdl {
    private String db;

    public static final SqlOperator SHOW =
            new SqlSpecialOperator("USE", SqlKind.OTHER_DDL);

    public SqlUse(SqlParserPos pos, String db) {
        super(SHOW, pos);
        this.db = db;
    }

    public String getDb() {
        return db;
    }

    @Nonnull
    @Override
    public List<SqlNode> getOperandList() {
        return null;
    }
}
