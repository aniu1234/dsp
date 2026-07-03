package com.qinyadan.system.dsp.engine;


public enum ShowEnum {

    /**
     * show tables;
     */
    SHOW_TABLBS(0, "tables"),

    /**
     * show databases;
     */
    SHOW_DBS(1, "databases"),

    /**
     * show create table
     */
    SHOW_CREATE(2, "create");

    private final int index;
    private final String startKeyWord;

    ShowEnum(int index, String startKeyWord) {
        this.index = index;
        this.startKeyWord = startKeyWord;
    }
}
