package com.qinyadan.system.dsp.protocol.pkg;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ResultSetHolder {

    private String schema;

    private String table;

    private List<Integer> columnType;

    //may be we can use pool data array to avoid frequent allocated space
    private List<List<String>> data;

    private String[] columnName;

}
