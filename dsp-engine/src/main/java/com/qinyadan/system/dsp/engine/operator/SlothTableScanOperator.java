package com.qinyadan.system.dsp.engine.operator;

import com.google.common.collect.Sets;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.data.type.DataType;
import com.qinyadan.system.dsp.engine.calcite.SlothSchemaHolder;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.storage.api.query.BaseQuery;
import com.qinyadan.system.dsp.storage.api.query.QueryContext;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.prepare.RelOptTableImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.Iterator;
import java.util.List;

public class SlothTableScanOperator extends AbstractOperator<SlothRow> {

    private RelOptTable table;

    private List<RexNode> project;

    private RexNode condition;

    /**
     * if project is not null or empty, the outputType is the project output type;
     */
    private RelDataType outputType;

    //MOCK
    private Iterator<SlothRow> iterator;

    private List<DataType> dataTypes;

    public SlothTableScanOperator(RelOptTable table, RelDataType originType,
                                  RelDataType outputType, List<RexNode> projects, RexNode filter) {
        super(originType);
        this.table = table;
        this.project = projects;
        this.condition = filter;
        this.outputType = outputType;
    }

    @Override
    public void open() {
        final RelOptTableImpl relOptTable = (RelOptTableImpl) table;
        final List<String> dbAndTable = relOptTable.getQualifiedName();
        final SlothTable slothTable = (SlothTable) SlothSchemaHolder.INSTANCE
                .getSlothSchema(dbAndTable.get(0)).getTable(dbAndTable.get(1));

        final SlothTableEngine slothTableEngine = slothTable.getSlothTableEngine();

        java.util.Set<String> columns = Sets.newHashSet(slothTableEngine.getColumnNames());
        final QueryContext queryContext = new QueryContext(
                BaseQuery.builder().columnNames(columns).build(), columns);
        iterator = slothTableEngine.search(queryContext);
    }

    @Override
    public SlothRow next() {
        while (iterator.hasNext()) {
            return iterator.next();
        }
        return SlothRow.EOF_ROW;
    }

    @Override
    public void close() {

    }
}
