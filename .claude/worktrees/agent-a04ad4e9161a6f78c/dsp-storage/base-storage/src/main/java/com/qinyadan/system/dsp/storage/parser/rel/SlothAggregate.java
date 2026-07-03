package com.qinyadan.system.dsp.storage.parser.rel;

import com.qinyadan.system.dsp.runtime.engine.data.SlothRow;
import com.qinyadan.system.dsp.storage.parser.rel.operator.Operator;
import com.qinyadan.system.dsp.storage.parser.rel.operator.SlothAggregateOperator;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.List;


public class SlothAggregate extends Aggregate implements SlothRel<SlothRow> {

    public SlothAggregate(RelOptCluster cluster, RelTraitSet traitSet, List<RelHint> hints, RelNode input,
                          ImmutableBitSet groupSet, List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
        super(cluster, traitSet, hints, input, groupSet, groupSets, aggCalls);
    }

    @Override
    public Aggregate copy(RelTraitSet traitSet, RelNode input, ImmutableBitSet groupSet,
                          List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
        return new SlothAggregate(getCluster(), traitSet, getHints(), input, groupSet, groupSets, aggCalls);
    }

    @Override
    public Operator<SlothRow> implement() {
        final Operator<SlothRow> input = ((SlothRel) getInput()).implement();

        //select id from person group by id
        //select distict id from person;
        //TODO
        final RelDataType rowType = getRowType();
        return new SlothAggregateOperator(input, groupSet, groupSets, aggCalls, rowType);
    }
}
