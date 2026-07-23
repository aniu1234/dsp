package com.qinyadan.system.dsp.schema.file.json;

import com.qinyadan.system.dsp.schema.file.FileSchemaFactory;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;


public class JsonFileSchemaFactory extends FileSchemaFactory {

    @Override
    protected TableTypeEnum tableType(String schemaName) {
        return TableTypeEnum.JSON;
    }
}
