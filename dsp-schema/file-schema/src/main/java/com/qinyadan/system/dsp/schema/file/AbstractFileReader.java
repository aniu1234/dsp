package com.qinyadan.system.dsp.schema.file;

import com.qinyadan.system.dsp.schema.common.util.FieldTypeEnum;
import com.qinyadan.system.dsp.schema.common.util.TypeConversionUtils;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public abstract class AbstractFileReader {

    protected final Path dataFilePath;
    private final List<String> columnNames;
    private final List<FieldTypeEnum> fieldTypes;

    protected AbstractFileReader(String dataFilePath, String typeFilePath) {
        this.dataFilePath = Paths.get(dataFilePath);
        Path schemaFilePath = Paths.get(typeFilePath);
        if (!Files.isRegularFile(this.dataFilePath)) {
            throw new IllegalArgumentException("Data file does not exist: " + dataFilePath);
        }
        if (!Files.isRegularFile(schemaFilePath)) {
            throw new IllegalArgumentException("Schema file does not exist: " + typeFilePath);
        }

        List<String> names = new ArrayList<>();
        List<FieldTypeEnum> types = new ArrayList<>();
        loadColumns(schemaFilePath, names, types);
        columnNames = Collections.unmodifiableList(names);
        fieldTypes = Collections.unmodifiableList(types);
    }

    public abstract Iterator<Object[]> readData();

    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        List<RelDataType> columnTypes = new ArrayList<>(fieldTypes.size());
        for (FieldTypeEnum fieldType : fieldTypes) {
            columnTypes.add(typeFactory.createJavaType(fieldType.getJavaClass()));
        }
        return typeFactory.createStructType(columnTypes, columnNames);
    }

    protected Object[] convertRow(List<?> values) {
        if (values.size() != fieldTypes.size()) {
            throw new IllegalArgumentException("Expected " + fieldTypes.size()
                    + " columns but found " + values.size() + " in " + dataFilePath);
        }
        Object[] row = new Object[fieldTypes.size()];
        for (int i = 0; i < fieldTypes.size(); i++) {
            row[i] = TypeConversionUtils.toObject(fieldTypes.get(i), values.get(i));
        }
        return row;
    }

    protected Object[] convertRow(Map<String, ?> values) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        Object[] row = new Object[fieldTypes.size()];
        for (int i = 0; i < fieldTypes.size(); i++) {
            String columnName = columnNames.get(i);
            row[i] = TypeConversionUtils.toObject(fieldTypes.get(i),
                    normalized.get(columnName.toLowerCase(Locale.ROOT)));
        }
        return row;
    }

    private void loadColumns(Path schemaFile, List<String> names,
                             List<FieldTypeEnum> types) {
        Set<String> normalizedNames = new HashSet<>();
        try {
            for (String line : Files.readAllLines(schemaFile, StandardCharsets.UTF_8)) {
                String definition = line.trim();
                if (definition.isEmpty() || definition.startsWith("#")) {
                    continue;
                }
                String[] nameAndType = definition.split(":", 2);
                if (nameAndType.length != 2 || nameAndType[0].trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid field definition '"
                            + definition + "' in " + schemaFile);
                }
                String name = nameAndType[0].trim();
                String normalizedName = name.toLowerCase(Locale.ROOT);
                if (!normalizedNames.add(normalizedName)) {
                    throw new IllegalArgumentException("Duplicate field '" + name
                            + "' in " + schemaFile);
                }
                names.add(name);
                types.add(FieldTypeEnum.getByType(nameAndType[1]));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read schema file: " + schemaFile, e);
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Schema file has no fields: " + schemaFile);
        }
    }
}
