package com.tomclaw.imageloader.core;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import it.pgp.xfiles.utils.Misc;

public class ImageLoaderImpl implements ImageLoader {

    public final FileProvider fileProvider;
    public final List<Decoder> decoders;
    public final MemoryCache memoryCache;
    public final Executor mainExecutor;
    public final ExecutorService backgroundExecutor;

    public final Map<String, Future<?>> futures = new HashMap<>();

    public ImageLoaderImpl(FileProvider fileProvider, List<Decoder> decoders, MemoryCache memoryCache, Executor mainExecutor, ExecutorService backgroundExecutor) {
        this.fileProvider = fileProvider;
        this.decoders = decoders;
        this.memoryCache = memoryCache;
        this.mainExecutor = mainExecutor;
        this.backgroundExecutor = backgroundExecutor;
    }

    @Override
    public <T> void load(ViewHolder<T> view, String uriString, Handlers<T> handlers) throws NoSuchAlgorithmException {
        ViewSize size = view.optSize();
        if(size == null) {
            waitSizeAsync(view, uriString, handlers);
            return;
        }
        String key = generateKey(uriString, size.width, size.height);
        Object prevTag = view.getTag();
        view.setTag(key);
        boolean isLoading = false;
        if(prevTag instanceof String) {
            String prevKey = (String) prevTag;
            Future<?> future = futures.get(prevKey);
            if(prevKey.equals(key) && (future != null && !future.isDone())) {
                isLoading = true;
            }
            else {
                if(future != null) future.cancel(true);
                isLoading = false;
            }
        }
        if(isLoading) return;

        Result value = memoryCache.get(key);
        if(value == null || value.isRecycled())
            loadAsync(view, size, uriString, key, handlers);
        else handlers.success.fn(view, value);
    }

    private <T> void waitSizeAsync(
            ViewHolder<T> viewHolder,
            String uriString,
            Handlers<T> handlers) {
        backgroundExecutor.submit(()->{
            viewHolder.getSize(); // not sure about implicit return in kotlin equivalent code
            mainExecutor.execute(()-> {
                try {
                    load(viewHolder, uriString, handlers);
                }
                catch(NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private <T> void loadAsync(
            ViewHolder<T> view,
            ViewSize size,
            String uriString,
            String key,
            Handlers<T> handlers
    ) {
        WeakReference<ViewHolder<T>> weakImageView = new WeakReference<>(view);
        handlers.placeholder.fn(view);
        Future<?> f = backgroundExecutor.submit(() -> {
            File file = fileProvider.getFile(uriString);
            if(file != null) {
                Decoder decoder = null;
                for(Decoder d: decoders) {
                    if(d.probe(file)) {
                        decoder = d;
                        break;
                    }
                }
                if(decoder != null) {
                    Result result = decoder != null ? decoder.decode(file, size.width, size.height) : null;
                    if(result != null) {
                        memoryCache.put(key, result);
                        mainExecutor.execute(() -> {
                            ViewHolder<T> weakImageViewVal = weakImageView.get();
                            if(weakImageViewVal != null) {
                                if(weakImageViewVal.getTag().equals(key)) {
                                    handlers.success.fn(view, result);
                                }
                            }
                            futures.remove(key);
                        });
                    }
                }
            }
            else handlers.error.fn(view);
        });
        futures.put(uriString, f);
    }

    private String generateKey(String url, int width, int height) throws NoSuchAlgorithmException {
        return toSHA1(url) + "_" + width + "_" + height;
    }

    static String toSHA1(String s) throws NoSuchAlgorithmException {
        byte[] bytes = MessageDigest.getInstance("SHA-1").digest(s.getBytes());
        return Misc.toHexString(bytes);
    }
}
