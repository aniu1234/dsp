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

    private String sqlState;

    private String errorMessage;


    @Override
    public void write(ByteBuf byteBuf) {
        IOUtils.writeByte(header, byteBuf);
        IOUtils.writeShort(errorCode, byteBuf);
        IOUtils.writeByte((byte) '#', byteBuf);
        String state = sqlState == null ? "HY000" : sqlState;
        if (state.length() != 5) {
            throw new IllegalArgumentException("SQLSTATE must contain exactly five characters");
        }
        IOUtils.writeBytesWithoutEndFlag(state.getBytes(java.nio.charset.StandardCharsets.US_ASCII), byteBuf);
        if (errorMessage != null) {
            IOUtils.writeBytesWithoutEndFlag(
                    errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8), byteBuf);
        }
    }
}
