package com.qinyadan.system.dsp.storage.parser.rules.converter;

import com.qinyadan.system.dsp.engine.rel.SlothProject;
import com.qinyadan.system.dsp.engine.calcite.SlothConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;


public class SlothProjectConverterRule extends AbstactSlothConverter {
    public static final SlothProjectConverterRule INSTANCE =
            new SlothProjectConverterRule(
                    LogicalProject.class,
                    Convention.NONE,
                    SlothConvention.INSTANCE,
                    "SlothProjectConverter");


    public SlothProjectConverterRule(Class<? extends RelNode> clazz,
                                     RelTrait in, Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
    }

    @Override
    public RelNode convert(RelNode rel) {
        LogicalProject logicalProject = (LogicalProject) rel;
        return new SlothProject(
                logicalProject.getCluster(),
                logicalProject.getTraitSet().replace(out),
                convert(logicalProject.getInput(), out),
                logicalProject.getProjects(),
                logicalProject.getRowType());
    }
}
