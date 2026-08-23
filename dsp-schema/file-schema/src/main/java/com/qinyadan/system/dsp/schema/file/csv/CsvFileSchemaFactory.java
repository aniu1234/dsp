package com.qinyadan.system.dsp.schema.file.csv;

import com.qinyadan.system.dsp.schema.common.spi.ConnectorHealth;
import com.qinyadan.system.dsp.schema.common.spi.SchemaConnector;
import com.qinyadan.system.dsp.schema.file.FileSchemaFactory;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;

import java.util.Map;


public class CsvFileSchemaFactory extends FileSchemaFactory implements SchemaConnector {

    @Override
    public String id() {
        return "file-csv";
    }

    @Override
    public ConnectorHealth health(Map<String, Object> operand) {
        return health(operand, TableTypeEnum.CSV);
    }

    @Override
    protected TableTypeEnum tableType(String schemaName) {
        return TableTypeEnum.CSV;
    }
}
