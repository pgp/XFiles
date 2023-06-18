package com.tomclaw.imageloader.core;

import android.util.LruCache;

public class MemoryCacheImpl implements MemoryCache {

    private final LruCache<String, Result> bitmapLruCache;

    public MemoryCacheImpl() {
        int maxMemory = (int)Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 12;
        bitmapLruCache = new LruCache<String, Result>(cacheSize) {
            @Override
            protected int sizeOf(String key, Result value) {
                return value.getByteCount();
            }
        };
    }

    @Override
    public Result get(String key) {
        return bitmapLruCache.get(key);
    }

    @Override
    public Result put(String key, Result result) {
        return bitmapLruCache.put(key, result);
    }

    @Override
    public Result remove(String key) {
        return bitmapLruCache.remove(key);
    }
}
