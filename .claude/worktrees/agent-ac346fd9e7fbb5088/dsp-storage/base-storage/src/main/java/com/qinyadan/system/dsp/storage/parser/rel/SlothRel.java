package com.qinyadan.system.dsp.storage.parser.rel;

import com.qinyadan.system.dsp.storage.parser.rel.operator.Operator;
import org.apache.calcite.rel.RelNode;


public interface SlothRel<R> extends RelNode {

    //add some implement method

    default Operator<R> implement() {
        return null;
    }
}
