package com.qinyadan.system.dsp.engine.rules.converter;


import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;


public abstract class AbstactSlothConverter extends ConverterRule {
    protected final Convention out;

    public AbstactSlothConverter(Class<? extends RelNode> clazz, RelTrait in,
                                 Convention out, String descriptionPrefix) {
        super(clazz, in, out, descriptionPrefix);
        this.out = out;
    }
}
