package com.qinyadan.system.dsp.schema.file.csv;

import com.qinyadan.system.dsp.schema.file.FileSchemaFactory;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;


public class CsvFileSchemaFactory extends FileSchemaFactory {

    @Override
    protected TableTypeEnum tableType(String schemaName) {
        return TableTypeEnum.CSV;
    }
}
