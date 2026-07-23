package com.qinyadan.system.dsp.engine.rel;

import com.qinyadan.system.dsp.engine.operator.Operator;
import org.apache.calcite.rel.RelNode;


public interface SlothRel<R> extends RelNode {

    Operator<R> implement();
}
