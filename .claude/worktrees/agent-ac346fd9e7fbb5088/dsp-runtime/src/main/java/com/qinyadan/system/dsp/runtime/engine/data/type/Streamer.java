package com.qinyadan.system.dsp.runtime.engine.data.type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface Streamer<T> {
    T readValueFrom(InputStream in) throws IOException;

    void writeValueTo(OutputStream out) throws IOException;
}
