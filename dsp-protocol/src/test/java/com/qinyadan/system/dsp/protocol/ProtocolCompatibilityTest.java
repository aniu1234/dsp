package com.qinyadan.system.dsp.protocol;

import com.qinyadan.system.dsp.constant.CapabilityFlags;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.utils.IOUtils;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ProtocolCompatibilityTest {

    @Test
    public void serverDoesNotAdvertiseTransactionsBeforeTheyAreImplemented() {
        assertEquals(0, PackageUtils.getServerCapality() & CapabilityFlags.CLIENT_TRANSACTIONS);
    }

    @Test
    public void errorPacketContainsProtocol41SqlStateWithoutNullTerminator() {
        MysqlPackage error = PackageUtils.buildErrPackage(1047, "Unknown command");
        ByteBuf buffer = PackageUtils.packageToBuf(error);
        try {
            int payloadLength = buffer.readUnsignedMediumLE();
            buffer.readByte();
            assertEquals(payloadLength, buffer.readableBytes());
            assertEquals(0xff, buffer.readUnsignedByte());
            assertEquals(1047, buffer.readUnsignedShortLE());
            assertEquals('#', buffer.readUnsignedByte());
            assertEquals("HY000", buffer.readCharSequence(5, StandardCharsets.US_ASCII).toString());
            String message = buffer.readCharSequence(buffer.readableBytes(), StandardCharsets.UTF_8).toString();
            assertEquals("Unknown command", message);
            assertFalse(message.endsWith("\u0000"));
        } finally {
            buffer.release();
        }
    }

    @Test
    public void lengthEncodedStringsUseUtf8ByteLength() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            IOUtils.writeLengthEncodedString(buffer, "中文-result");
            assertEquals("中文-result", IOUtils.readLengthEncodedString(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void lengthEncodedIntegersUseProtocolBoundaryPrefixes() {
        assertLengthEncoding(250, 250);
        assertLengthEncoding(251, 0xfc);
        assertLengthEncoding(65535, 0xfc);
        assertLengthEncoding(65536, 0xfd);
        assertLengthEncoding(16777215, 0xfd);
        assertLengthEncoding(16777216, 0xfe);
    }

    private static void assertLengthEncoding(int value, int prefix) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            IOUtils.writeLengthEncodedInteger(value, buffer);
            assertEquals(prefix, buffer.getUnsignedByte(0));
            assertEquals(value, IOUtils.readLengthEncodedInteger(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
