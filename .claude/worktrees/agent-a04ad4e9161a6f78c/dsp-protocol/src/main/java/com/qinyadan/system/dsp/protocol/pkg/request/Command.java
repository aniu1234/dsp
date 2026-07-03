package com.qinyadan.system.dsp.protocol.pkg.request;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;


@Data
public class Command extends AbstractReaderAndWriter {
    private byte commandType;

    private String command;

    @Override
    public void read(ByteBuf byteBuf) {
        this.commandType = IOUtils.readByte(byteBuf);
        this.command = IOUtils.readEofString(byteBuf);
    }
}
