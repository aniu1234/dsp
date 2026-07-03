package com.qinyadan.system.dsp.storage;

import com.qinyadan.system.dsp.storage.parser.ParserFactory;
import com.qinyadan.system.dsp.storage.parser.SlothParser;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.Test;

public class ParserFactoryTest {

    @Test
    public void testParserFactory() throws SqlParseException {
        SlothParser slothParser = ParserFactory.getParser("show databases", null);
        SqlNode sqlNode = slothParser.getSqlNode();

        System.out.println(sqlNode.getKind());
    }
}
