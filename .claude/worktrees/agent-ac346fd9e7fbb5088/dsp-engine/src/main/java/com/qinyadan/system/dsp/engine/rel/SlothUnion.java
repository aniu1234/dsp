package com.qinyadan.system.dsp.engine.rel;

import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;
import com.qinyadan.system.dsp.engine.operator.SlothUnionOperator;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.SetOp;
import org.apache.calcite.rel.core.Union;

import java.util.List;
import java.util.stream.Collectors;


public class SlothUnion extends Union implements SlothRel<SlothRow> {
    public SlothUnion(RelOptCluster cluster, RelTraitSet traits,
                      List<RelNode> inputs, boolean all) {
        super(cluster, traits, inputs, all);
    }

    @Override
    public SetOp copy(RelTraitSet traitSet, List<RelNode> inputs, boolean all) {
        return new SlothUnion(getCluster(), traitSet, inputs, all);
    }

    @Override
    public Operator<SlothRow> implement() {
        final List<Operator<SlothRow>> inputs = getInputs().stream()
                .map(r -> ((SlothRel<SlothRow>) r).implement())
                .collect(Collectors.toList());

        return new SlothUnionOperator(getRowType(), inputs, all);
    }
}
