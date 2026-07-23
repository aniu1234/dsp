package com.qinyadan.system.dsp.protocol.utils;

import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.ResultSetHolder;
import com.qinyadan.system.dsp.protocol.pkg.auth.ServerGreeting;
import com.qinyadan.system.dsp.protocol.pkg.io.ReaderAndWriter;
import com.qinyadan.system.dsp.protocol.pkg.response.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.List;
import java.util.Arrays;

import static com.qinyadan.system.dsp.constant.CapabilityFlags.*;
import static com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum.SYNTAX_ERROR;


public class PackageUtils {

    public static final String AUTHENCATION_PLUGIN = "mysql_native_password";
    private static final int SERVER_VERSION = 0x0a;
    private static final String MYSQL_SERVER_VERSION = "5.7.22";

    public static ServerGreeting buildInitAuthencatinPackage(byte[] challenge) {
        if (challenge == null || challenge.length != 20) {
            throw new IllegalArgumentException("MySQL authentication challenge must contain 20 bytes");
        }

        byte[] saltOne = Arrays.copyOfRange(challenge, 0, 8);
        byte[] saltTwo = new byte[13];
        System.arraycopy(challenge, 8, saltTwo, 0, 12);

        int serverCapability = getServerCapality();
        byte[] sereverCapacility = IOUtils.getBytes(serverCapability);
        byte[] lower = new byte[]{sereverCapacility[0], sereverCapacility[1]};
        byte[] higher = new byte[]{sereverCapacility[2], sereverCapacility[3]};

        ServerGreeting greetingPackage = ServerGreeting.builder()
                .serverThreadId((int) Thread.currentThread().getId())
                .saltOne(saltOne)
                .protocalVeriosn((byte) SERVER_VERSION)
                .serverVeriosnInfo(MYSQL_SERVER_VERSION)
                //origin is 0xff, cause only disable-ssl mysql -h127.0.0.1 -P3016 -uroot -p123456 --ssl-mode=disabled can connect
                .serverCapability((short) (serverCapability & 0x0000ffff))
                .extendServerCapabilities((short) ((serverCapability >> 16) & 0x0000ffff))
                //see https://dev.mysql.com/doc/internals/en/character-set.html#packet-Protocol::CharacterSet
                .charSet((byte) 33)
                .serverStatus((short) 2)
                .authencationPluginLength((byte) AUTHENCATION_PLUGIN.length())
                .saltTwo(saltTwo)
                .authencationPlugin(AUTHENCATION_PLUGIN)
                .build();

        return greetingPackage;
    }

    public static int getServerCapality() {
        int flags = 0;

        flags |= CLIENT_LONG_PASSWORD;
        flags |= CLIENT_FOUND_ROWS;
        flags |= CLIENT_LONG_FLAG;
        flags |= CLIENT_CONNECT_WITH_DB;
        flags |= CLIENT_ODBC;
        flags |= CLIENT_IGNORE_SPACE;
        flags |= CLIENT_PROTOCOL_41;
        flags |= CLIENT_INTERACTIVE;
        flags |= CLIENT_IGNORE_SIGPIPE;
        flags |= CLIENT_SECURE_CONNECTION;
        flags |= CLIENT_PLUGIN_AUTH;
        return flags;
    }


    public static MysqlPackage buildOkMySqlPackage(int affectedRows, int seqNumber, int lastInsertId) {
        OkPackage okPackage = OkPackage.builder()
                .header((byte) 0x00)
                .serverStatus(0x0002)
                .affectedRows(affectedRows)
                .lastInsertId(lastInsertId)
                .build();

        MysqlPackage mysqlPacakge = new MysqlPackage(okPackage);
        mysqlPacakge.setSeqNumber((byte) seqNumber);

        return mysqlPacakge;
    }

    public static MysqlPackage buildErrPackage(int errorCode, String errorMessage) {
        return buildErrPackage(errorCode, errorMessage, 1);
    }

    public static MysqlPackage buildErrPackage(int errorCode, String errorMessage, int seqNumber) {
        ErrPackage errPackage = ErrPackage.builder()
                .header((byte) 0xff)
                .errorCode((short) errorCode)
                .sqlState("HY000")
                .errorMessage(errorMessage)
                .build();

        MysqlPackage mysqlPacakge = new MysqlPackage(errPackage);
        mysqlPacakge.setSeqNumber((byte) seqNumber);
        return mysqlPacakge;
    }


    public static MysqlPackage buildSyntaxErrPackage(String query) {
        return buildErrPackage(SYNTAX_ERROR.getCode(), String.format(SYNTAX_ERROR.getMessage(), query), 1);
    }

    public static ByteBuf packageToBuf(ReaderAndWriter readerAndWriter) {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(128);
        readerAndWriter.write(byteBuf);

        return byteBuf;
    }

    public static ByteBuf packageToBuf(ReaderAndWriter readerAndWriter, ByteBuf byteBuf) {
        readerAndWriter.write(byteBuf);
        return byteBuf;
    }

    /**
     * 构造ResultSet包
     *
     * @param resultSetHolder
     * @return
     */
    public static ByteBuf buildResultSet(ResultSetHolder resultSetHolder) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(128);
        int sequence = writeResultSetHeader(resultSetHolder, buffer, 1);
        for (List<String> row : resultSetHolder.getData()) {
            sequence = writeResultSetRow(row, buffer, sequence);
        }
        writeResultSetEnd(buffer, sequence);
        return buffer;
    }

    public static int writeResultSetHeader(ResultSetHolder resultSetHolder,
                                           ByteBuf buffer, int sequence) {
        final List<Integer> columnType = resultSetHolder.getColumnType();
        final String[] columnName = resultSetHolder.getColumnName();
        final int columnNum = columnType.size();
        final MysqlPackage columnCountPackage = new MysqlPackage(
                new ColumnCount(columnNum));
        columnCountPackage.setSeqNumber((byte) sequence++);
        columnCountPackage.write(buffer);

        for (int i = 0; i < columnNum; i++) {
            final MysqlPackage columnTypeMysqlPackage = new MysqlPackage();
            final ColumnType columnTypePackage =
                    ColumnType.builder()
                            .catalog("def")
                            .schema(resultSetHolder.getSchema())
                            .table(resultSetHolder.getTable())
                            .orgTable(resultSetHolder.getTable())
                            .name(columnName[i])
                            .originalName(columnName[i])
                            //original 33
                            .charSet(33)
                            .filler((byte) 0x0c)
                            //original 84
                            .columnLength(1024)
                            .columnType((byte) columnType.get(i).intValue())
                            .flags(0x00)
                            .dicimals((byte) 0x00)
                            .build();

            columnTypeMysqlPackage.setAbstractReaderAndWriterPackage(columnTypePackage);
            columnTypeMysqlPackage.setSeqNumber((byte) sequence++);
            columnTypeMysqlPackage.write(buffer);
        }
        final MysqlPackage eofPackage = new MysqlPackage(new EofPackage((byte) 0xfe, 0, 0x0002));
        eofPackage.setSeqNumber((byte) sequence++);
        eofPackage.write(buffer);
        return sequence;
    }

    public static int writeResultSetRow(List<String> row, ByteBuf buffer, int sequence) {
        MysqlPackage columnValue = new MysqlPackage(new ColumnValue(row));
        columnValue.setSeqNumber((byte) sequence++);
        columnValue.write(buffer);
        return sequence;
    }

    public static int writeResultSetEnd(ByteBuf buffer, int sequence) {
        final MysqlPackage eofPackage = new MysqlPackage(new EofPackage((byte) 0xfe, 0, 0x0002));
        eofPackage.setSeqNumber((byte) sequence++);
        eofPackage.write(buffer);
        return sequence;
    }
}
