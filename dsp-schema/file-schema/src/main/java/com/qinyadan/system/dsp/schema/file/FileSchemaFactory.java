package com.qinyadan.system.dsp.schema.file;

import com.qinyadan.system.dsp.schema.common.config.SchemaConfig;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FileSchemaFactory implements SchemaFactory {
    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        String directory = SchemaConfig.requireString(operand, "directory");
        Object rawBaseDirectory = operand == null ? null : operand.get("baseDirectory");
        if (rawBaseDirectory == null) {
            throw new IllegalArgumentException("Schema option 'baseDirectory' is required");
        }
        Path baseDirectory = rawBaseDirectory instanceof File
                ? ((File) rawBaseDirectory).toPath()
                : Paths.get(rawBaseDirectory.toString());
        return new FileSchema(baseDirectory.resolve(directory), tableType(name));
    }

    protected TableTypeEnum tableType(String schemaName) {
        return TableTypeEnum.getTableTypeEnumByName(schemaName);
    }
}
