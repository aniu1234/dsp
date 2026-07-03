package com.qinyadan.system.dsp.storage.parser;

import com.qinyadan.system.dsp.storage.parser.rel.SlothRel;
import org.apache.calcite.plan.*;


/**
 * 执行计划定义
 */
public class SlothConvention implements Convention {

    public static final SlothConvention INSTANCE = new SlothConvention();

    @Override
    public Class getInterface() {
        return SlothRel.class;
    }

    @Override
    public String getName() {
        return "Sloth";
    }

    @Override
    public boolean canConvertConvention(Convention toConvention) {
        return false;
    }

    @Override
    public boolean useAbstractConvertersForConversion(RelTraitSet fromTraits, RelTraitSet toTraits) {
        return false;
    }

    @Override
    public RelTraitDef getTraitDef() {
        return ConventionTraitDef.INSTANCE;
    }

    @Override
    public boolean satisfies(RelTrait trait) {
        return this == trait;
    }

    @Override
    public void register(RelOptPlanner planner) {

    }

    @Override
    public String toString() {
        return "SlothConvention";
    }
}
