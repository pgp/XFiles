package com.tomclaw.imageloader.core;

import android.net.Uri;

import java.io.File;
import java.util.HashMap;


public final class FileProviderImpl implements FileProvider {
    private final File cacheDir;
    private final DiskCache diskCache;
    private final HashMap<String, Loader> loaders = new HashMap<>();

    public FileProviderImpl(File cacheDir, DiskCache diskCache, Loader... loaders) {
        this.cacheDir = cacheDir;
        this.diskCache = diskCache;
        for(Loader loader : loaders)
            for(String s : loader.getSchemes())
                this.loaders.put(s, loader);
    }

    @Override
    public File getFile(Uri uri) {
        File f = diskCache.get(uri.toString());
        if(f != null) return f;
        return loadIntoCache(uri);
    }

    private File loadIntoCache(Uri uri) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("file", ".tmp", cacheDir);
            Loader loader = loaders.get(uri.getScheme());
            if(loader == null) return null;
            String uriString = uri.toString();
            if(loader.load(uriString, tempFile))
                return diskCache.put(uriString, tempFile);
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        finally {
            if(tempFile != null) tempFile.delete();
        }
        return null;
    }
}
