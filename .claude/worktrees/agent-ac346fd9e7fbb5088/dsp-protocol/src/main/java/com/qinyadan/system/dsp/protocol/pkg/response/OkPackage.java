package com.qinyadan.system.dsp.protocol.pkg.response;

import com.qinyadan.system.dsp.protocol.pkg.AbstractReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class OkPackage extends AbstractReaderAndWriter {

    /**
     * 0x00 is ok
     * 0xfe is EOF
     */
    private byte header;

    //add later
    private int affectedRows;

    //add later
    private int lastInsertId;

    private int serverStatus;

    //add later
    private int numberOfWarning;

    private String info;

    @Override
    public void write(ByteBuf byteBuf) {
        IOUtils.writeByte(header, byteBuf);
        IOUtils.writeInteger(affectedRows, byteBuf, 1);
        IOUtils.writeInteger(lastInsertId, byteBuf, 1);
        IOUtils.writeInteger(serverStatus, byteBuf, 2);
        IOUtils.writeInteger(numberOfWarning, byteBuf, 2);

        if (info != null) {
            IOUtils.writeLengthEncodedString(byteBuf, info);
        }
    }
}
