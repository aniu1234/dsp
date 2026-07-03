package com.qinyadan.system.dsp.storage.parser.ddl;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.annotation.Nonnull;
import java.util.List;


public class SqlCreateDb extends SqlCreate {

    public static final SqlSpecialOperator CREATE_DATABASE =
            new SqlSpecialOperator("CREATE DATABASE", SqlKind.OTHER_DDL);

    private String dbName;

    public String getDbName() {
        return dbName;
    }

    public SqlCreateDb(SqlParserPos pos,
                       boolean replace,
                       boolean ifNotExists,
                       String dbName) {
        super(CREATE_DATABASE, pos, replace, ifNotExists);
        this.dbName = dbName;
    }

    @Nonnull
    @Override
    public List<SqlNode> getOperandList() {
        return null;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        super.unparse(writer, leftPrec, rightPrec);
    }
}
