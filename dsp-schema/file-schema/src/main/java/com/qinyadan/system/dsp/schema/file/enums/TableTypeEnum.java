package com.qinyadan.system.dsp.schema.file.enums;

public enum TableTypeEnum {
    /**
     *
     */
    CSV(".csv"),

    /**
     *
     */
    JSON(".json");

    private final String dataSuffix;

    TableTypeEnum(String dataSuffix) {
        this.dataSuffix = dataSuffix;
    }

    public String getDataSuffix() {
        return dataSuffix;
    }

    public static TableTypeEnum getTableTypeEnumByName(String name) {
        for (TableTypeEnum tableTypeEnum : TableTypeEnum.values()) {
            if (tableTypeEnum.name().equalsIgnoreCase(name)) {
                return tableTypeEnum;
            }
        }

        throw new IllegalArgumentException(String.format("Unsupport table type '%s'", name));
    }
}
