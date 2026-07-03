package com.qinyadan.system.dsp.engine.rules.converter;

import com.qinyadan.system.dsp.engine.rel.SlothSort;
import com.qinyadan.system.dsp.engine.calcite.SlothConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalSort;


public class SlothSortConverterRule extends AbstactSlothConverter {

    public static final SlothSortConverterRule INSTANCE = new SlothSortConverterRule(
            LogicalSort.class,
            Convention.NONE,
            SlothConvention.INSTANCE,
            "SlothSortConverterRule"
    );

    public SlothSortConverterRule(Class<? extends RelNode> clazz, RelTrait in, Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
    }

    @Override
    public RelNode convert(RelNode rel) {
        LogicalSort logicalSort = (LogicalSort) rel;

        return new SlothSort(
                logicalSort.getCluster(),
                logicalSort.getTraitSet().replace(out),
                convert(logicalSort.getInput(), out),
                logicalSort.getCollation(),
                logicalSort.offset,
                logicalSort.fetch);
    }
}
