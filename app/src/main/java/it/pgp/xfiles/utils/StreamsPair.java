package it.pgp.xfiles.utils;

import java.io.DataInputStream;
import java.io.OutputStream;

import it.pgp.xfiles.roothelperclient.RootHelperClient;

/**
 * Common supertype for {@link it.pgp.xfiles.roothelperclient.RemoteManager}
 * and {@link RootHelperClient.RootHelperStreams}
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
