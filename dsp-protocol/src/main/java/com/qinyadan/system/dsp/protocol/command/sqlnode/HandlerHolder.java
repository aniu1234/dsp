package com.qinyadan.system.dsp.protocol.command.sqlnode;


import com.qinyadan.system.dsp.engine.parser.ddl.*;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDelete;
import org.apache.calcite.sql.SqlInsert;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUpdate;

import java.util.HashMap;
import java.util.Map;


public class HandlerHolder {

    public static final Map<Class<?>, Handler<?>> SQL_TYPE_TO_HANDLER_MAP = new HashMap<>();

    static {
        SQL_TYPE_TO_HANDLER_MAP.put(SqlUse.class, SqlUseHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlShow.class, SqlShowHandler.INSTANCE);

        SQL_TYPE_TO_HANDLER_MAP.put(SqlSelect.class, SqlSelectHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlOrderBy.class, SqlSelectHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlBasicCall.class, SqlSelectHandler.INSTANCE);

        SQL_TYPE_TO_HANDLER_MAP.put(SqlDrop.class, SqlDropHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlCreateDb.class, SqlCreateDbHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlCreateTable.class, SqlCreateTableHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlSet.class, SqlSetHandler.INSTANCE);

        SQL_TYPE_TO_HANDLER_MAP.put(SqlInsert.class, SqlInsertHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlUpdate.class, SqlMutationHandler.INSTANCE);
        SQL_TYPE_TO_HANDLER_MAP.put(SqlDelete.class, SqlMutationHandler.INSTANCE);
    }
}
