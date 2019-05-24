package it.pgp.xfiles.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a continuous (not necessarily atomic) socket write, made of a group of smaller writes;
 * the actual write to the enclosed outputstream happens on full buffer and on close(),
 * in so allowing to use try with resources
 * It is actually a BufferedOutputStream with close() disabled (calls only flush() instead)
 */

public class FlushingBufferedOutputStream extends BufferedOutputStream {

    public FlushingBufferedOutputStream(OutputStream out) {
        super(out);
    }

    public FlushingBufferedOutputStream(OutputStream out, int size) {
        super(out, size);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
