package it.pgp.xfiles.utils;

import java.io.DataInputStream;
import java.io.OutputStream;

/**
 * Common supertype for {@link it.pgp.xfiles.roothelperclient.RemoteManager}
 * and {@link it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent.RootHelperStreams}
 */
public abstract class StreamsPair implements AutoCloseable {

    public DataInputStream i;
    public OutputStream o;

    @Override
    public void close() {
        try {i.close();} catch (Exception ignored) {}
        try {o.close();} catch (Exception ignored) {}
    }
}
