package com.qinyadan.system.dsp.schema.file;

import com.qinyadan.system.dsp.schema.common.constants.CommonConstant;
import com.qinyadan.system.dsp.schema.file.csv.CsvBaseFileTable;
import com.qinyadan.system.dsp.schema.file.csv.CsvFileReader;
import com.qinyadan.system.dsp.schema.file.enums.TableTypeEnum;
import com.qinyadan.system.dsp.schema.file.json.JsonBaseFileTable;
import com.qinyadan.system.dsp.schema.file.json.JsonFileReader;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileSchema extends AbstractSchema {

    private final Map<String, Table> tableMap;

    public FileSchema(String baseDirectory, String directory) {
        this(Paths.get(baseDirectory).resolve(directory),
                TableTypeEnum.getTableTypeEnumByName(directory));
    }

    public FileSchema(Path directory, TableTypeEnum tableType) {
        if (directory == null || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Schema directory does not exist: " + directory);
        }
        if (tableType == null) {
            throw new IllegalArgumentException("Table type must not be null");
        }
        tableMap = Collections.unmodifiableMap(loadTables(directory, tableType));
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }

    private Map<String, Table> loadTables(Path directory, TableTypeEnum tableType) {
        Map<String, Table> tables = new LinkedHashMap<>();
        for (Path schemaFile : schemaFiles(directory)) {
            String fileName = schemaFile.getFileName().toString();
            String tableName = fileName.substring(0,
                    fileName.length() - CommonConstant.SCHEMA_SUFFIX.length());
            Path dataFile = directory.resolve(tableName + tableType.getDataSuffix());
            if (!Files.isRegularFile(dataFile)) {
                throw new IllegalArgumentException("Data file does not exist for table '"
                        + tableName + "': " + dataFile);
            }
            tables.put(tableName, createTable(tableType, dataFile, schemaFile));
        }
        return tables;
    }

    private List<Path> schemaFiles(Path directory) {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString()
                            .endsWith(CommonConstant.SCHEMA_SUFFIX))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read schema directory: " + directory, e);
        }
    }

    private Table createTable(TableTypeEnum tableType, Path dataFile, Path schemaFile) {
        switch (tableType) {
            case CSV:
                return new CsvBaseFileTable(new CsvFileReader(
                        dataFile.toString(), schemaFile.toString()));
            case JSON:
                return new JsonBaseFileTable(new JsonFileReader(
                        dataFile.toString(), schemaFile.toString()));
            default:
                throw new IllegalArgumentException("Unsupported table type " + tableType);
        }
    }
}
