package com.qinyadan.system.dsp.storage.parser.rules.converter;

import com.qinyadan.system.dsp.engine.rel.SlothTableScan;
import com.qinyadan.system.dsp.engine.calcite.SlothConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;

public class SlothTableScanConverterRule extends AbstactSlothConverter {

    public static final SlothTableScanConverterRule INSTANCE = new SlothTableScanConverterRule(
            LogicalTableScan.class,
            Convention.NONE,
            SlothConvention.INSTANCE,
            "SlothTableScanConverterRule"
    );

    public SlothTableScanConverterRule(Class<? extends RelNode> clazz, RelTrait in,
                                       Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalTableScan logicalTableScan = (LogicalTableScan) rel;

        return new SlothTableScan(
                logicalTableScan.getCluster(),
                logicalTableScan.getTraitSet().replace(out),
                rel.getTable());
    }
}
