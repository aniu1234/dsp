package com.qinyadan.system.dsp.runtime.engine;

import org.apache.calcite.schema.Table;

import javax.annotation.PostConstruct;


public class TableStorgeEngineManger {

    private Table table;

    private RealTimeEngine realTimeEngine;

    private BlockEngine blockEngine;

    @PostConstruct
    public void init() {
        //load data to restore blockEngine and realtime Engine
    }
}
