package com.qinyadan.system.dsp.engine.calcite.rules.converter;

import com.qinyadan.system.dsp.engine.rel.SlothUnion;
import com.qinyadan.system.dsp.engine.calcite.SlothConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalUnion;

import java.util.List;
import java.util.stream.Collectors;


public class SlothUnionConverterRule extends AbstactSlothConverter {

    public static final SlothUnionConverterRule INSTANCE = new SlothUnionConverterRule(
            LogicalUnion.class,
            Convention.NONE,
            SlothConvention.INSTANCE,
            "SlothUnionConverterRule"
    );

    public SlothUnionConverterRule(Class<? extends RelNode> clazz, RelTrait in, Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalUnion logicalUnion = (LogicalUnion) rel;

        final List<RelNode> inputs = logicalUnion.getInputs().stream()
                .map(r -> convert(r, out))
                .collect(Collectors.toList());

        return new SlothUnion(
                logicalUnion.getCluster(),
                logicalUnion.getTraitSet().replace(out),
                inputs,
                logicalUnion.all);
    }
}
