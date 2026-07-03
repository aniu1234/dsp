package com.qinyadan.system.dsp.protocol.pkg.response;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;

import java.util.List;


public class ColumnValue extends AbstractReaderAndWriter {

    //all value encode with LengthEncodedString
    private List<String> texts;

    public ColumnValue(List<String> texts) {
        this.texts = texts;
    }

    @Override
    public void write(ByteBuf byteBuf) {
        texts.forEach(t -> IOUtils.writeLengthEncodedString(byteBuf, t));
    }
}
