package it.pgp.xfiles.service.params;

import java.io.Serializable;

/**
 * Created by pgp on 05/11/17
 */

public class DownloadParams implements Serializable {
    public String url;
    public String destPath; // dir where to download remote file (LocalPathContent, unwrapped)
    public String filename; // desired filename, if null, will try to get remote filename

    public DownloadParams(String url, String destPath, String filename) {
        this.url = url;
        this.destPath = destPath;
        this.filename = filename;
    }
}
