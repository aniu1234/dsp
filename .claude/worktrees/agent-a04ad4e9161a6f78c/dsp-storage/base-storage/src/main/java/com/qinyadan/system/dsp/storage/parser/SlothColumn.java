package com.qinyadan.system.dsp.storage.parser;


public class SlothColumn {

    private String columnName;

    private EnhanceSlothColumn columnType;

    public SlothColumn(String columnName, EnhanceSlothColumn columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }

    public String getColumnName() {
        return columnName;
    }

    public EnhanceSlothColumn getColumnType() {
        return columnType;
    }

    //Others to be add;
}
