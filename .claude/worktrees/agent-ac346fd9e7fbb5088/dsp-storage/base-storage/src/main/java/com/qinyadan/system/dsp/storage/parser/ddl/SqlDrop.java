package com.qinyadan.system.dsp.storage.parser.ddl;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.annotation.Nonnull;
import java.util.List;


public class SqlDrop extends SqlDdl {
    private String name;
    private boolean exist;

    //true is drop db, false is drop table
    private boolean dropDb;

    public String getName() {
        return name;
    }

    public boolean isExist() {
        return exist;
    }

    public boolean isDropDb() {
        return dropDb;
    }

    //should split drop table and drop db;
    public static final SqlOperator DROP =
            new SqlSpecialOperator("DROP", SqlKind.OTHER_DDL);


    public SqlDrop(SqlParserPos pos, boolean exist, String name, boolean dropDb) {
        super(DROP, pos);
        this.name = name;
        this.exist = exist;
        this.dropDb = dropDb;
    }

    @Nonnull
    @Override
    public List<SqlNode> getOperandList() {
        return null;
    }
}
