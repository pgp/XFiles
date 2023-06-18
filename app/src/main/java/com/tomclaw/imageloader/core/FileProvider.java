package com.tomclaw.imageloader.core;

import android.net.Uri;

import java.io.File;


public interface FileProvider {
    File getFile(Uri uri);

    default File getFile(String url) {
        return getFile(Uri.parse(url));
    }
}
