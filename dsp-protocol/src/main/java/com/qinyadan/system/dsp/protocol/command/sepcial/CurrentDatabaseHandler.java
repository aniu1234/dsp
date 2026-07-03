package com.qinyadan.system.dsp.protocol.command.sepcial;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.constant.ColumnTypeConstants;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


public class CurrentDatabaseHandler extends BaseHandler {

    public static final CurrentDatabaseHandler INSTANCE = new CurrentDatabaseHandler();

    public static final String CURRENT_DATABASE_RESULT_COLUMN = "database()";

    @Override
    public void handle(ConnectionContext connectionContext, String type) {
        String db = connectionContext.getDb();

        final List<List<String>> data = Lists.newArrayList();
        data.add(Lists.newArrayList(db));

        final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                .columnName(new String[]{CURRENT_DATABASE_RESULT_COLUMN})
                .columnType(Lists.newArrayList(ColumnTypeConstants.MYSQL_TYPE_VAR_STRING))
                .data(data)
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();

        final ByteBuf byteBuf = PackageUtils.buildResultSet(resultSetHolder);
        connectionContext.write(byteBuf);
    }
}
