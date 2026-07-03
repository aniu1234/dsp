package com.qinyadan.system.dsp.schema.file.json;

import com.qinyadan.system.dsp.schema.file.AbstractFileReader;
import com.qinyadan.system.dsp.schema.file.BaseFileTable;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;


public class JsonBaseFileTable extends BaseFileTable {
    public JsonBaseFileTable(AbstractFileReader fileReader) {
        super(fileReader);
    }

    @Override
    public String toString() {
        return "JsonFileTable";
    }

    @Override
    public Statistic getStatistic() {
        return Statistics.UNKNOWN;
    }


}
