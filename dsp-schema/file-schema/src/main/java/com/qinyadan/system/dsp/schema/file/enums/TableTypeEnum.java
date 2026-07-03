package com.qinyadan.system.dsp.schema.file.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum TableTypeEnum {
    /**
     *
     */
    CSV(0, "CSV"),

    /**
     *
     */
    JSON(1, "JSON");

    private final int index;
    private final String name;

    public static TableTypeEnum getTableTypeEnumByName(String name) {
        for (TableTypeEnum tableTypeEnum : TableTypeEnum.values()) {
            if (tableTypeEnum.name.equalsIgnoreCase(name)) {
                return tableTypeEnum;
            }
        }

        throw new IllegalArgumentException(String.format("Unsupport table type '%s'", name));
    }
}
