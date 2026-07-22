package com.qinyadan.system.dsp.storage.api;

import com.qinyadan.system.dsp.storage.api.meta.ColumnInfo;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class EngineConfig {
    String storagePath;
    List<ColumnInfo> columns;
    Map<String, Object> properties;
}
