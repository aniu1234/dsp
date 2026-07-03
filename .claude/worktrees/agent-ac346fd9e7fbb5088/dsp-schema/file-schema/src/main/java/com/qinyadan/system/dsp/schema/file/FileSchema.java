package com.qinyadan.system.dsp.schema.file;

import com.google.common.collect.ImmutableMap;
import com.qinyadan.system.dsp.schema.common.constants.CommonConstant;
import com.qinyadan.system.dsp.schema.file.csv.CsvBaseFileTable;
import com.qinyadan.system.dsp.schema.file.csv.CsvFileReader;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;
import com.qinyadan.system.dsp.schema.file.json.JsonBaseFileTable;
import com.qinyadan.system.dsp.schema.file.json.JsonFileReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class FileSchema extends AbstractSchema {

    private String dirPath;
    private String schema;

    private Map<String, Table> tableMap;

    public FileSchema(String dirPath, String scheam) {
        this.dirPath = dirPath;
        this.schema = scheam;
    }

    @Override
    protected Map<String, Table> getTableMap() {

        if (tableMap != null && !tableMap.isEmpty()) {
            return tableMap;
        }

        final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();
        final String schemaDirPath = dirPath + File.separator + schema;
        final File file = new File(schemaDirPath);
        Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(CommonConstant.SCHEMA_SUFFIX))
                .map(f -> f.getName().substring(0, f.getName().indexOf("."))).forEach(fileName -> {

                    try {
                        final TableTypeEnum tableTypeEnum = TableTypeEnum.getTableTypeEnumByName(schema);
                        final String filePath = schemaDirPath + File.separator + fileName;

                        BaseFileTable table = null;
                        if (TableTypeEnum.JSON == tableTypeEnum) {
                            final JsonFileReader jsonFileReader =
                                    new JsonFileReader(filePath + CommonConstant.JSON_SUFFIX, filePath + CommonConstant.SCHEMA_SUFFIX);
                            table = new JsonBaseFileTable(jsonFileReader);

                        } else if (TableTypeEnum.CSV == tableTypeEnum) {
                            final CsvFileReader csvFileReader =
                                    new CsvFileReader(filePath + CommonConstant.CSV_SUFFIX, filePath + CommonConstant.SCHEMA_SUFFIX);
                            table = new CsvBaseFileTable(csvFileReader);
                        }

                        if (null != table) {
                            builder.put(fileName, table);
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                });

        tableMap = builder.build();

        return tableMap;
    }
}
