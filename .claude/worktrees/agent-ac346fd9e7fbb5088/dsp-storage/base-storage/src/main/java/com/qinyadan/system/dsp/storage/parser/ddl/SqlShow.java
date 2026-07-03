package com.qinyadan.system.dsp.storage.parser.ddl;

import com.qinyadan.system.dsp.runtime.engine.ShowEnum;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.annotation.Nonnull;
import java.util.List;


public class SqlShow extends SqlDdl {

    private String command;
    private ShowEnum type;

    public String getCommand() {
        return command;
    }

    public SqlShow(SqlParserPos pos, ShowEnum type, String command) {
        super(SHOW, pos);
        this.type = type;
        this.command = command;
    }

    public static final SqlOperator SHOW =
            new SqlSpecialOperator("SHOW", SqlKind.OTHER_DDL);

    public ShowEnum getType() {
        return type;
    }

    @Nonnull
    @Override
    public List<SqlNode> getOperandList() {
        return null;
    }
}
