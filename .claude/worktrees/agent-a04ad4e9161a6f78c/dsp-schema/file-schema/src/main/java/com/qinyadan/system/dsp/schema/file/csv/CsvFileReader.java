package com.qinyadan.system.dsp.schema.file.csv;

import com.google.common.base.Throwables;
import com.qinyadan.system.dsp.schema.common.util.TypeConvertionUtils;
import com.qinyadan.system.dsp.schema.file.AbstractFileReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.util.Iterator;


@Getter
@Slf4j
public class CsvFileReader extends AbstractFileReader {
    public CsvFileReader(String dataFilePath, String typeFilePath) {
        super(dataFilePath, typeFilePath);
    }

    @Override
    public Iterator<Object[]> readData() {
        try {

            final CSVFormat csvFormat = CSVFormat.DEFAULT
                    .withQuoteMode(QuoteMode.NON_NUMERIC)
                    .withQuote('\'')
                    .withIgnoreSurroundingSpaces()
                    .withTrailingDelimiter()
                    .withNullString("null");

            final CSVParser csvParser = new CSVParser(new FileReader(dataFilePath), csvFormat);
            return csvParser.getRecords().stream().map(record -> {
                final int columnSize = record.size();
                final Object[] res = new Object[columnSize];
                for (int i = 0; i < columnSize; i++) {
                    res[i] = TypeConvertionUtils.toObject(fieldTypeEnums.get(i), record.get(i));
                }

                return res;
            }).iterator();
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

}
