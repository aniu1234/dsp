package com.qinyadan.system.dsp.storage.api;

import com.qinyadan.system.dsp.core.data.type.DataType;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class EngineConfig {
    String storagePath;
    int shardNum;
    List<DataType<?>> columnTypes;
    Map<String, Object> properties;
}
