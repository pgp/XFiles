package com.tomclaw.imageloader;

import android.content.Context;

import com.tomclaw.cache.DiskLruCache;
import com.tomclaw.imageloader.core.Decoder;
import com.tomclaw.imageloader.core.DiskCacheImpl;
import com.tomclaw.imageloader.core.FileProvider;
import com.tomclaw.imageloader.core.FileProviderImpl;
import com.tomclaw.imageloader.core.ImageLoader;
import com.tomclaw.imageloader.core.ImageLoaderImpl;
import com.tomclaw.imageloader.core.MainExecutorImpl;
import com.tomclaw.imageloader.core.MemoryCache;
import com.tomclaw.imageloader.core.MemoryCacheImpl;
import com.tomclaw.imageloader.util.BitmapDecoder;
import com.tomclaw.imageloader.util.loader.ContentLoader;
import com.tomclaw.imageloader.util.loader.FileLoader;
import com.tomclaw.imageloader.util.loader.UrlLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleImageLoader {

    private static ImageLoader imageLoader;

    // converted from extension function
    public static ImageLoader getImageLoader(Context context) {
        try {
            return imageLoader != null ? imageLoader : initImageLoader(context,null,null,null,null,null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // converted from extension function
    public static ImageLoader initImageLoader(Context context, List<Decoder> decoders, FileProvider fileProvider, MemoryCache memoryCache, Executor mainExecutor, ExecutorService backgroundExecutor) throws IOException {
        if(decoders == null) decoders = Collections.singletonList(new BitmapDecoder());
        if(fileProvider == null) fileProvider = new FileProviderImpl(
                context.getCacheDir(),
                new DiskCacheImpl(DiskLruCache.create(context.getCacheDir(), 15728640L)),
                new UrlLoader(),
                new FileLoader(context.getAssets()),
                new ContentLoader(context.getContentResolver())
        );
        if(memoryCache == null) memoryCache = new MemoryCacheImpl();
        if(mainExecutor == null) mainExecutor = new MainExecutorImpl();
        if(backgroundExecutor == null) backgroundExecutor = Executors.newFixedThreadPool(10);
        ImageLoaderImpl loader = new ImageLoaderImpl(
                fileProvider,
                decoders,
                memoryCache,
                mainExecutor,
                backgroundExecutor
        );
        imageLoader = loader;
        return loader;
    }
}
