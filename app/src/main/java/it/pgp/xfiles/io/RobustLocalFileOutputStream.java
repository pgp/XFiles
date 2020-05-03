package it.pgp.xfiles.io;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import it.pgp.xfiles.MainActivity;

public class RobustLocalFileOutputStream extends OutputStream {

    OutputStream o;

    public RobustLocalFileOutputStream(String path) throws IOException {
        try {
            o = new FileOutputStream(new File(path));
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("XFiles-IO", getClass().getName()+": open for writing failed in in-app mode, trying with roothelper-proxy file streams...");
        }
        try {
            o = MainActivity.getRootHelperClient().getOutputStream(path);
        }
        catch(NullPointerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        o.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        o.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        o.flush();
    }

    @Override
    public void write(int b) throws IOException {
        o.write(b);
    }

    @Override
    public void close() throws IOException {
        o.close();
    }
}
