package com.qinyadan.system.dsp.engine.rel;

import com.qinyadan.system.dsp.engine.operator.Operator;
import org.apache.calcite.rel.RelNode;


public interface SlothRel<R> extends RelNode {

    //add some implement method

    default Operator<R> implement() {
        return null;
    }
}
