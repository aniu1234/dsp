package com.qinyadan.system.dsp.storage.api.meta;

import com.qinyadan.system.dsp.core.data.type.DataType;
import lombok.Value;

import java.util.List;

/**
 * Immutable description of a table column.
 */
@Value
public class ColumnInfo {
    String name;
    DataType<?> type;
    boolean nullable;
    Object defaultValue;
    String comment;
}
