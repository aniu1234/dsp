package com.qinyadan.system.dsp.protocol.pkg.response;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class EofPackage extends AbstractReaderAndWriter {
    private byte eof = (byte) 0xfe;

    //2 byte
    private int warningCount;

    //2 byte
    private int status;


    @Override
    public void read(ByteBuf byteBuf) {
        this.eof = byteBuf.readByte();

        if (byteBuf.isReadable()) {
            this.warningCount = IOUtils.readInteger(byteBuf, 2);
            this.status = IOUtils.readInteger(byteBuf, 2);
        }
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeByte(eof);

        IOUtils.writeInteger(warningCount, byteBuf, 2);
        IOUtils.writeInteger(status, byteBuf, 2);
    }
}
