package com.qinyadan.system.dsp.storage.parser.rel;

import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.runtime.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.storage.parser.rel.operator.Operator;
import com.qinyadan.system.dsp.storage.parser.rel.operator.SlothProjectOperator;
import com.qinyadan.system.dsp.runtime.util.RexShuttleUtils;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.List;


public class SlothProject extends Project implements SlothRel<SlothRow> {

    public SlothProject(RelOptCluster cluster, RelTraitSet traits, RelNode input, List<? extends RexNode> projects, RelDataType rowType) {
        super(cluster, traits, input, projects, rowType);
    }

    @Override
    public Project copy(RelTraitSet traitSet, RelNode input, List<RexNode> projects, RelDataType rowType) {
        return new SlothProject(getCluster(), traitSet, input, projects, rowType);
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        return super.computeSelfCost(planner, mq).multiplyBy(0.1);
    }


    @Override
    public Operator<SlothRow> implement() {
        final Operator<SlothRow> child = ((SlothRel<SlothRow>) input).implement();

        //todo
        final List<Symbol> symbols = RexShuttleUtils.rexToSymbox(this.exps);
        final RelDataType dataType = getRowType();
        return new SlothProjectOperator(child, symbols, dataType);
    }
}
