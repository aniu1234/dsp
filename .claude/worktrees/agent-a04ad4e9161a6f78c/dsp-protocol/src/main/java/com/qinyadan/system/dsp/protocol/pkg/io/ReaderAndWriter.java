package com.qinyadan.system.dsp.protocol.pkg.io;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;


public interface ReaderAndWriter extends Serializable {
    /**
     * Read content from {@link ByteBuf}
     *
     * @param byteBuf
     */
    default void read(ByteBuf byteBuf) {
        //TODO
    }


    /**
     * Write content to {@link ByteBuf}
     *
     * @param byteBuf
     */
    default void write(ByteBuf byteBuf) {
        //TODO
    }
}
