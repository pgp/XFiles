package com.tomclaw.imageloader.core;

import android.support.annotation.Nullable;

import com.tomclaw.cache.RecordNotFoundException;

import java.io.File;
import java.io.IOException;

public interface DiskCache {

    @Nullable File get(String key);

    File put(String key, File file) throws IOException;

    void remove(String key) throws IOException, RecordNotFoundException;

}
