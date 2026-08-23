package com.qinyadan.system.dsp.protocol.visitor;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.engine.service.EnvironmentService;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlShuttle;

import java.util.Objects;
import java.util.Locale;


public class EnvironmentReplaceVisitor extends SqlShuttle {

    private ConnectionContext connectionContext;

    public EnvironmentReplaceVisitor(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public SqlNode visit(SqlIdentifier id) {
        final String name = id.toString();

        if (name.startsWith("@@")) {
            String key = name.substring(2).toLowerCase(Locale.ROOT);
            if (key.startsWith("session.")) {
                key = key.substring("session.".length());
            } else if (key.startsWith("global.")) {
                key = key.substring("global.".length());
            } else if (key.startsWith("local.")) {
                key = key.substring("local.".length());
            }

            String value = connectionContext.getProperties().get(key);
            if (Objects.isNull(value)) {
                value = EnvironmentService.INSTANCE.getGlobal(key);
                if (Objects.isNull(value)) {
                    throw new UnsupportedOperationException("Can't suppport environment value '" + key + "'");
                }
            }

            final SqlLiteral environmentValue =
                    SqlLiteral.createCharString(value, null, SqlParserPos.ZERO);

            SqlIdentifier sqlIdentifier = new SqlIdentifier(Lists.newArrayList(key), SqlParserPos.ZERO);
            return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, environmentValue, sqlIdentifier);
        }

        return super.visit(id);
    }
}
