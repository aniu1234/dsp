package com.qinyadan.system.dsp.protocol.pkg;

import com.qinyadan.system.dsp.protocol.pkg.io.ReaderAndWriter;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Data;
import lombok.ToString;


/**
 * https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_packets.html#sect_protocol_basic_packets_packet
 * https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_command_phase.html
 *
 * @see
 */
@Data
@ToString
public class MysqlPackage implements ReaderAndWriter {

    /**
     *
     */
    public interface Protocol {

        public static final byte COM_QUIT = 0x01;
        /**
         * USE
         */
        public static final byte COM_INIT_DB = 0x02;
        /**
         * 包含：大部分查询执行语句
         * SELECT
         * CRETAE DATABASE
         * DROP DATABASE
         * SHOW
         * DROP TABLE
         */
        public static final byte COM_QUERY = 0x03;

        public static final byte COM_XXX = 0x04;

        public static final byte COM_SET_OPTION = 0x1A;

        public static final byte COM_PING = 0x0E;

        public static final byte COM_STATISTICS = 0x08;

        public static final byte COM_PROCESS_INFO = 0x0A;

        public static final byte COM_PROCESS_KILL = 0x0C;
    }

    /**
     * Length of Message body
     */
    private int lengthOfMessage;

    /**
     * Sequence number
     */
    private byte seqNumber;

    /**
     * Package contennt
     */
    private AbstractReaderAndWriter abstractReaderAndWriterPackage;


    public MysqlPackage() {
    }

    public MysqlPackage(AbstractReaderAndWriter abstractReaderAndWriterPackage) {
        this.abstractReaderAndWriterPackage = abstractReaderAndWriterPackage;
    }


    @Override
    public void read(ByteBuf byteBuf) {
        //read and then
        this.lengthOfMessage = IOUtils.readInteger(byteBuf, 3);
        this.seqNumber = IOUtils.readByte(byteBuf);
        abstractReaderAndWriterPackage.read(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {

        ByteBuf tmp = PooledByteBufAllocator.DEFAULT.buffer(128);
        abstractReaderAndWriterPackage.write(tmp);

        this.lengthOfMessage = tmp.readableBytes();
        IOUtils.writeInteger3(lengthOfMessage, byteBuf);
        IOUtils.writeByte(seqNumber, byteBuf);
        byte[] bytes = new byte[tmp.readableBytes()];
        tmp.readBytes(bytes);

        //change from writeBytes -->  writeBytesWithoutEndFlag
        IOUtils.writeBytesWithoutEndFlag(bytes, byteBuf);
    }
}
