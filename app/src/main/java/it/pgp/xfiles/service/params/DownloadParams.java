package it.pgp.xfiles.service.params;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by pgp on 05/11/17
 */

public class DownloadParams implements Serializable {
    public String url;
    public String destPath; // dir where to download remote file (LocalPathContent, unwrapped)
    public String filename; // desired filename, if null, will try to get remote filename

    public DownloadParams(@NonNull String url, @Nullable String destPath) {
        this.url = url;
        this.destPath = destPath;
    }

    public DownloadParams(@NonNull String url, @Nullable String destPath, @Nullable String filename) {
        this.url = url;
        this.destPath = destPath;
        this.filename = filename;
    }
}
