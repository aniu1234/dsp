package com.qinyadan.system.dsp.protocol.pkg.response;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ErrPackage extends AbstractReaderAndWriter {
    private byte header;

    private short errorCode;

    private String errorMessage;


    @Override
    public void write(ByteBuf byteBuf) {
        IOUtils.writeByte(header, byteBuf);
        IOUtils.writeShort(errorCode, byteBuf);
        IOUtils.writeString(errorMessage, byteBuf);
    }
}
