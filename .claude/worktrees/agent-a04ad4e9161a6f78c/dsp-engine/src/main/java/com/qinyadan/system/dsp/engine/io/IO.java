package com.qinyadan.system.dsp.engine.io;

import io.netty.buffer.ByteBuf;


public interface IO {

    /**
     * write Object to buffer
     *
     * @param byteBuf
     */
    default void write(ByteBuf byteBuf) {
    }

    /**
     * Read object from buffer
     *
     * @param byteBuf
     */
    default void read(ByteBuf byteBuf) {
    }
}
