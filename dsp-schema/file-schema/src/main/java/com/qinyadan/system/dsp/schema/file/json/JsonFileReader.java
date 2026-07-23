package com.qinyadan.system.dsp.schema.file.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinyadan.system.dsp.schema.file.AbstractFileReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class JsonFileReader extends AbstractFileReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> ROW_TYPE =
            new TypeReference<Map<String, Object>>() { };

    public JsonFileReader(String dataFilePath, String typeFilePath) {
        super(dataFilePath, typeFilePath);
    }

    @Override
    public Iterator<Object[]> readData() {
        List<Object[]> rows = new ArrayList<>();
        try (InputStream input = Files.newInputStream(dataFilePath);
             MappingIterator<Map<String, Object>> iterator = OBJECT_MAPPER
                     .readerFor(ROW_TYPE).readValues(input)) {
            while (iterator.hasNextValue()) {
                rows.add(convertRow(iterator.nextValue()));
            }
            return rows.iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON file: " + dataFilePath, e);
        }
    }
}
