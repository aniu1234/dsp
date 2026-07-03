package com.qinyadan.system.dsp.storage.parser.rel;

import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.storage.parser.rel.operator.Operator;
import com.qinyadan.system.dsp.storage.parser.rel.operator.SlothFilterOperator;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;



public class SlothFilter extends Filter implements SlothRel<SlothRow> {

    public SlothFilter(RelOptCluster cluster, RelTraitSet traits, RelNode child, RexNode condition) {
        super(cluster, traits, child, condition);
    }

    @Override
    public Filter copy(RelTraitSet traitSet, RelNode input, RexNode condition) {
        return new SlothFilter(input.getCluster(), traitSet, input, condition);
    }

    @Override
    public Operator<SlothRow> implement() {
        final Operator<SlothRow> input = ((SlothRel<SlothRow>) getInput()).implement();
        RelDataType relDataType = this.getRowType();
        RexNode condition = this.condition;

        return new SlothFilterOperator(condition, input, relDataType);
    }
}
