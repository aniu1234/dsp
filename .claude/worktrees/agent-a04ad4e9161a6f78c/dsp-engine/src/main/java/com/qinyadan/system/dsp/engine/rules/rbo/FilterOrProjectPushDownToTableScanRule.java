package com.qinyadan.system.dsp.storage.parser.rules.rbo;

import com.qinyadan.system.dsp.engine.rel.SlothFilter;
import com.qinyadan.system.dsp.engine.rel.SlothProject;
import com.qinyadan.system.dsp.engine.rel.SlothTableScan;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rex.RexNode;

import java.util.List;


public abstract class FilterOrProjectPushDownToTableScanRule extends RelOptRule {

    public static final FilterOrProjectPushDownToTableScanRule PROJECT_PUSH_DOWN_INSTANCE = new FilterOrProjectPushDownToTableScanRule(
            operand(SlothProject.class, operand(SlothTableScan.class, none())),
            "ProjectPushDownToScanRule") {

        @Override
        public boolean matches(RelOptRuleCall call) {
            //return super.matches(call);

            return false;
        }

        @Override
        public void onMatch(RelOptRuleCall call) {
            final SlothProject slothProject = call.rel(0);
            final SlothTableScan slothTableScan = call.rel(1);

            final List<RexNode> projects = slothProject.getProjects();
            slothTableScan.setProjects(projects);

            final SlothTableScan r = new SlothTableScan(
                    slothTableScan.getCluster(),
                    slothTableScan.getTraitSet(),
                    slothTableScan.getTable()
            );

            r.setProjects(projects);
            r.setOutputType(slothProject.getRowType());

            call.transformTo(r);
        }
    };


    public static final FilterOrProjectPushDownToTableScanRule FILTER_PUSH_DOWN_INSTANCE = new FilterOrProjectPushDownToTableScanRule(
            operand(SlothFilter.class, operand(SlothTableScan.class, none())),
            "FilterPushDownToScanRule") {

        @Override
        public boolean matches(RelOptRuleCall call) {
            final SlothFilter slothFilter = call.rel(0);
            RexNode rexNode = slothFilter.getCondition();

            //todo, and/or condition push to tablescan
            return false;
        }

        @Override
        public void onMatch(RelOptRuleCall call) {
            final SlothFilter slothFilter = call.rel(0);
            final SlothTableScan slothTableScan = call.rel(1);

            final RexNode condition = slothFilter.getCondition();
            slothTableScan.setCondition(condition);

            call.transformTo(slothTableScan);
        }
    };

    public FilterOrProjectPushDownToTableScanRule(RelOptRuleOperand operand, String description) {
        super(operand, description);
    }

}
