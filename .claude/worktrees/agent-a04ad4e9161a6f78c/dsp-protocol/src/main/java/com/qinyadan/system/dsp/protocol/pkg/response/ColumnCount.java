package com.qinyadan.system.dsp.protocol.pkg.response;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;


public class ColumnCount extends AbstractReaderAndWriter {

    private int columnCount;

    public ColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public void read(ByteBuf byteBuf) {
        columnCount = IOUtils.readLengthEncodedInteger(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        IOUtils.writeLengthEncodedInteger(columnCount, byteBuf);
    }
}
