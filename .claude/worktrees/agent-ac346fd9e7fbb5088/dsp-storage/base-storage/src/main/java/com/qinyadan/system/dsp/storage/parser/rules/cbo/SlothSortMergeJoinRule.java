package com.qinyadan.system.dsp.storage.parser.rules.cbo;

import com.qinyadan.system.dsp.storage.parser.PhysicalJoinType;
import com.qinyadan.system.dsp.storage.parser.rel.SlothJoin;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;


public class SlothSortMergeJoinRule extends RelOptRule {

    public static final SlothSortMergeJoinRule INSTANCE = new SlothSortMergeJoinRule(
            operand(SlothJoin.class, any()), "SlothSortMergeJoinRule");

    public SlothSortMergeJoinRule(RelOptRuleOperand operand, String description) {
        super(operand, description);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        SlothJoin slothJoin = call.rel(0);

        return slothJoin.getPhysicalNode() == PhysicalJoinType.NONE;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        SlothJoin slothJoin = call.rel(0);

        RelNode left = slothJoin.getLeft();
        RelNode right = slothJoin.getRight();

        //# 一个Collation
//        RexNode condition = slothJoin.getCondition();

        // Should enfore collation
//        RelTraitSet leftTrait = left.getTraitSet();
//        RelCollationImpl.of()
    }
}
