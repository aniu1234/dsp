package com.qinyadan.system.dsp.engine.calcite;


public enum PhysicalJoinType {

    /**
     *
     */
    NONE("None", 0),
    /**
     *
     */
    NEST_LOOP_JOIN("NestLoopJoin", 1),

    /**
     *
     */
    HASH_JOIN("HashJoin", 2),

    /**
     *
     */
    SORT_MERGE("SortMerge", 3);

    private final String name;
    private final int index;

    PhysicalJoinType(String name, int index) {
        this.name = name;
        this.index = index;
    }
}
