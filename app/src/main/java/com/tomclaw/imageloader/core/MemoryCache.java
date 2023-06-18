package com.tomclaw.imageloader.core;

import android.support.annotation.Nullable;

public interface MemoryCache {

    @Nullable Result get(String key);

    @Nullable Result put(String key, Result result);

    @Nullable Result remove(String key);

}
