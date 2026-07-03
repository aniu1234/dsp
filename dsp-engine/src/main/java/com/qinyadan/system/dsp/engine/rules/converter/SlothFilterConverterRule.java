package com.qinyadan.system.dsp.engine.rules.converter;

import com.qinyadan.system.dsp.engine.rel.SlothFilter;
import com.qinyadan.system.dsp.engine.calcite.SlothConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;


public class SlothFilterConverterRule extends AbstactSlothConverter {

    public static final SlothFilterConverterRule INSTANCE = new SlothFilterConverterRule(
            LogicalFilter.class,
            Convention.NONE,
            SlothConvention.INSTANCE,
            "SlothFilterConvertRule"

    );

    public SlothFilterConverterRule(Class<? extends RelNode> clazz, RelTrait in,
                                    Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalFilter logicalFilter = (LogicalFilter) rel;
        return new SlothFilter(
                logicalFilter.getCluster(),
                logicalFilter.getTraitSet().replace(out),
                //use convert instead
                convert(logicalFilter.getInput(), out),
                logicalFilter.getCondition());
    }
}
