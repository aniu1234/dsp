package com.qinyadan.system.dsp.engine.util;

import com.qinyadan.system.dsp.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.engine.rex.RexToSymbolShuttle;
import org.apache.calcite.rex.RexNode;

import java.util.List;
import java.util.stream.Collectors;


public class RexShuttleUtils {

    private static final RexToSymbolShuttle SHUTTLE = RexToSymbolShuttle.INSTANCE;

    public static List<Symbol> rexToSymbox(List<RexNode> rexNodes) {
        return rexNodes.stream()
                .map(rexNode -> rexNode.accept(SHUTTLE))
                .collect(Collectors.toList());
    }
}
