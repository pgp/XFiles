package it.pgp.xfiles.io;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;

public class RobustLocalFileInputStream extends InputStream {

    InputStream i;

    public RobustLocalFileInputStream(String path) throws IOException {
        try {
            i = new FileInputStream(new File(path));
            return;
        }
        catch (IOException e) {
            Log.e("XFiles-IO", getClass().getName()+": open for reading failed in in-app mode, trying with roothelper-proxy file streams...", e);
        }
        i = new RootHelperClientUsingPathContent().getInputStream(path);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return i.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return i.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return i.skip(n);
    }

    @Override
    public int available() throws IOException {
        return i.available();
    }

    @Override
    public void close() throws IOException {
        i.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        i.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        i.reset();
    }

    @Override
    public boolean markSupported() {
        return i.markSupported();
    }

    @Override
    public int read() throws IOException {
        return i.read();
    }
}
