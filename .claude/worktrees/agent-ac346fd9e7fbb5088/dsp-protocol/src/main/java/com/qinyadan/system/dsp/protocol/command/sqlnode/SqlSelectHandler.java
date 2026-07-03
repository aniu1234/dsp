package com.qinyadan.system.dsp.protocol.command.sqlnode;

import com.google.common.collect.Lists;
import com.qinyadan.system.dsp.constant.ColumnTypeConstants;
import com.qinyadan.system.dsp.protocol.command.sepcial.SpecialSelectHolder;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import com.qinyadan.system.dsp.protocol.visitor.EnvironmentReplaceVisitor;
import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import com.qinyadan.system.dsp.storage.parser.rel.operator.Operator;
import com.qinyadan.system.dsp.storage.parser.ParserFactory;
import com.qinyadan.system.dsp.storage.parser.SlothParser;
import com.qinyadan.system.dsp.storage.parser.rel.SlothRel;
import io.netty.buffer.ByteBuf;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class SqlSelectHandler implements Handler<SqlNode> {

    public static final SqlSelectHandler INSTANCE = new SqlSelectHandler();

    @Override
    public void handle(ConnectionContext connectionContext, SqlNode type) {

        String s = type.toString();
        Handler handler;
        if ((handler = SpecialSelectHolder.SPECIAL_HANDLER.get(s)) != null) {
            handler.handle(connectionContext, s);
            return;
        }

        SlothParser slothParser = ParserFactory.getParser(connectionContext.getQueryString(),
                connectionContext.getDb());
        type = type.accept(new EnvironmentReplaceVisitor(connectionContext));
        final RelNode relNode = slothParser.getPlan(type);

        final Operator<SlothRow> operator = ((SlothRel) relNode).implement();

        //now start to start execute;
        final List<List<Object>> result = executeOperator(operator);

        final List<List<String>> data = result.stream()
                .map(l -> l.stream().map(v -> Objects.isNull(v) ? null : v.toString()).collect(Collectors.toList()))
                .collect(Collectors.toList());

        final List<String> columnNames = relNode.getRowType().getFieldNames();
        final List<Integer> rowTypes = relNode.getRowType()
                .getFieldList()
                .stream()
                .map(f -> f.getType().getSqlTypeName())
                .map(ColumnTypeConstants::getMysqlType)
                .collect(Collectors.toList());

        final ResultSetHolder resultSetHolder = ResultSetHolder.builder()
                .columnName(columnNames.toArray(new String[0]))
                .columnType(rowTypes)
                .data(data)
                .schema(StringUtils.EMPTY)
                .table(StringUtils.EMPTY)
                .build();

        final ByteBuf byteBuf = PackageUtils.buildResultSet(resultSetHolder);
        connectionContext.write(byteBuf);
    }

    /**
     * Execute the Mpp iterator operator
     *
     * @param operator
     * @return
     */
    private List<List<Object>> executeOperator(Operator<SlothRow> operator) {
        final List<List<Object>> result = Lists.newArrayList();
        operator.open();

        SlothRow tmp;
        while ((tmp = operator.next()) != SlothRow.EOF_ROW) {
            result.add(tmp.getAllColumn().stream().map(Value::getValueByType).collect(Collectors.toList()));
        }

        return result;
    }
}
