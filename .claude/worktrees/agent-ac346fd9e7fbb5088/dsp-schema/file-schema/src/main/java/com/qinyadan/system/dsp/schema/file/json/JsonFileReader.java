package com.qinyadan.system.dsp.schema.file.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import com.qinyadan.system.dsp.schema.common.constants.CommonConstant;
import com.qinyadan.system.dsp.schema.common.util.TypeConvertionUtils;
import com.qinyadan.system.dsp.schema.file.AbstractFileReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Getter
@Slf4j
public class JsonFileReader extends AbstractFileReader {

    public JsonFileReader(String dataFilePath, String typeFilePath) {
        super(dataFilePath, typeFilePath);
    }

    @Override
    public Iterator<Object[]> readData() {
        try {
            final List<String> stringList =
                    IOUtils.readLines(new FileInputStream(dataFilePath), Charset.defaultCharset());
            return stringList.stream().map(json -> {
                try {
                    final JsonNode jsonNode = CommonConstant.OBJECT_MAPPER.readTree(json);
                    final List<String> c = new ArrayList<>(CommonConstant.OBJECT_MAPPER
                            .convertValue(jsonNode, new TypeReference<Map<String, String>>() {
                            })
                            .values());

                    final int size = c.size();
                    Object[] result = new Object[size];
                    for (int i = 0; i < size; i++) {
                        result[i] = TypeConvertionUtils.toObject(fieldTypeEnums.get(i), c.get(i));
                    }
                    return result;
                } catch (Exception e) {
                    log.error(Throwables.getStackTraceAsString(e));
                    throw new RuntimeException(e);
                }
            }).iterator();

        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }
}
