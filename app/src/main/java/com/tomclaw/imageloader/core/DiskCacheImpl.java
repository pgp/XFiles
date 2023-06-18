package com.tomclaw.imageloader.core;

import com.tomclaw.cache.DiskLruCache;
import com.tomclaw.cache.RecordNotFoundException;

import java.io.File;
import java.io.IOException;

public class DiskCacheImpl implements DiskCache {

    private final DiskLruCache diskLruCache;

    public DiskCacheImpl(DiskLruCache diskLruCache) {
        this.diskLruCache = diskLruCache;
    }

    @Override
    public File get(String key) {
        return diskLruCache.get(key);
    }

    @Override
    public File put(String key, File file) throws IOException {
        return diskLruCache.put(key, file);
    }

    @Override
    public void remove(String key) throws IOException, RecordNotFoundException {
        diskLruCache.delete(key);
    }
}
