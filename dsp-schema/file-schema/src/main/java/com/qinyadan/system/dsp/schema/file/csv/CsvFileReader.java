package com.qinyadan.system.dsp.schema.file.csv;

import com.qinyadan.system.dsp.schema.file.AbstractFileReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CsvFileReader extends AbstractFileReader {

    public CsvFileReader(String dataFilePath, String typeFilePath) {
        super(dataFilePath, typeFilePath);
    }

    @Override
    public Iterator<Object[]> readData() {
        CSVFormat format = CSVFormat.DEFAULT
                .withIgnoreSurroundingSpaces()
                .withNullString("null");
        List<Object[]> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(dataFilePath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>(record.size());
                for (String value : record) {
                    values.add(value);
                }
                rows.add(convertRow(values));
            }
            return rows.iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read CSV file: " + dataFilePath, e);
        }
    }
}
