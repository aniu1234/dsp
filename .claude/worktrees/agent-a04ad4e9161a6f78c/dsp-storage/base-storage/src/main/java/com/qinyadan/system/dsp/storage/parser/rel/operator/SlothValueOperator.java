package com.qinyadan.system.dsp.storage.parser.rel.operator;

import com.google.common.collect.ImmutableList;
import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.type.DataType;
import com.qinyadan.system.dsp.runtime.engine.data.value.Value;
import com.qinyadan.system.dsp.runtime.util.TypeConversionUtils;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


public class SlothValueOperator extends AbstractOperator<SlothRow> {

    private final ImmutableList<ImmutableList<RexLiteral>> values;

    //这里需要搞成Literal ???,
    private Iterator<SlothRow> r;

    public SlothValueOperator(ImmutableList<ImmutableList<RexLiteral>> values, RelDataType relDataType) {
        super(relDataType);
        this.values = values;
    }

    @Override
    public void open() {
        r = values.stream()
                .map(l -> {
                            List<Value> dataValues = l.stream()
                                    .map(r -> {
                                        final SqlTypeName sqlTypeName = r.getType().getSqlTypeName();
                                        DataType dataType = TypeConversionUtils.getBySqlTypeName(sqlTypeName);
                                        Object re1 = r.getValue();
                                        return new Value(re1, dataType);
                                    }).collect(Collectors.toList());
                            return new SlothRow(dataValues);
                        }
                ).collect(Collectors.toList())
                .iterator();

    }

    @Override
    public SlothRow next() {
        if (r.hasNext()) {
            return r.next();
        }

        return SlothRow.EOF_ROW;
    }

    @Override
    public void close() {

    }
}
