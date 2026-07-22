package com.qinyadan.system.dsp.protocol.pkg;

import com.qinyadan.system.dsp.protocol.pkg.netty.ConnectionContext;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.List;

/**
 * Writes a result set incrementally so query results are not retained in protocol memory.
 */
public class ResultSetStreamWriter implements AutoCloseable {

    private static final int FLUSH_THRESHOLD = 64 * 1024;

    private final ConnectionContext connectionContext;
    private ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(8192);
    private int sequence = 1;
    private boolean started;
    private boolean terminal;

    public ResultSetStreamWriter(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    public void start(ResultSetHolder metadata) {
        ensureWritable();
        if (started) {
            throw new IllegalStateException("Result set has already started");
        }
        sequence = PackageUtils.writeResultSetHeader(metadata, buffer, sequence);
        started = true;
        flushChunkIfNeeded();
    }

    public void writeRow(List<String> row) {
        ensureWritable();
        if (!started) {
            throw new IllegalStateException("Result set metadata has not been written");
        }
        sequence = PackageUtils.writeResultSetRow(row, buffer, sequence);
        flushChunkIfNeeded();
    }

    public void finish() {
        ensureWritable();
        sequence = PackageUtils.writeResultSetEnd(buffer, sequence);
        terminal = true;
        flushFinalChunk();
    }

    public void fail(MysqlPackage error) {
        ensureWritable();
        error.setSeqNumber((byte) sequence++);
        error.write(buffer);
        terminal = true;
        flushFinalChunk();
    }

    private void flushChunkIfNeeded() {
        if (buffer.readableBytes() >= FLUSH_THRESHOLD) {
            connectionContext.getChannelHandlerContext().write(buffer);
            buffer = PooledByteBufAllocator.DEFAULT.buffer(8192);
        }
    }

    private void flushFinalChunk() {
        connectionContext.getChannelHandlerContext().writeAndFlush(buffer);
        buffer = null;
    }

    private void ensureWritable() {
        if (terminal || buffer == null) {
            throw new IllegalStateException("Result set has already completed");
        }
    }

    @Override
    public void close() {
        if (!terminal && buffer != null) {
            buffer.release();
            buffer = null;
        }
    }
}
