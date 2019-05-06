package it.pgp.xfiles.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class NetworkBufferedChunk extends OutputStream implements AutoCloseable {

    private OutputStream o;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public NetworkBufferedChunk(OutputStream o) {
        this.o=o;
    }

    @Override
    public void write(int b) throws IOException {
        baos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        baos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        baos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        baos.flush();
    }

    @Override
    public void close() throws IOException {
        o.write(baos.toByteArray());
        o.flush();
    }
}
