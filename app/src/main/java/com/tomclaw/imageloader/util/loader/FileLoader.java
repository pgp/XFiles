package com.tomclaw.imageloader.util.loader;

import android.content.res.AssetManager;

import com.tomclaw.imageloader.core.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

public class FileLoader implements Loader {

    public static boolean safeCopyTo(InputStream safeCopyTo, OutputStream output) {
        try {
            copyTo(safeCopyTo, output);
            return true;
        }
        catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static void copyTo(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[10240];
        do {
            int read = inputStream.read(bArr);
            if(read == -1) return;
            outputStream.write(bArr, 0, read);
        }
        while(!Thread.interrupted());
        throw new InterruptedIOException();
    }

    private static final String ASSET_PREFIX = "/android_asset/";

    private final AssetManager assets;

    public FileLoader(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public List<String> getSchemes() {
        return Collections.singletonList("file");
    }

    @Override
    public boolean load(String uriString, File file) {
        URI uri;
        try {
            uri = new URI(uriString);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            if(uri.getPath().startsWith(ASSET_PREFIX)) {
                if(assets != null) input = assets.open(uri.getPath().replace(ASSET_PREFIX, ""));
            }
            else input = new FileInputStream(new File(uri));
            output = new FileOutputStream(file);
            safeCopyTo(input, output);
            return true;
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        finally {
            try {input.close();}
            catch(Exception ignored) {}
            try {output.close();}
            catch(Exception ignored) {}
        }
    }
}
