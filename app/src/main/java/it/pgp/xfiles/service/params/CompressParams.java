package it.pgp.xfiles.service.params;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

import it.pgp.xfiles.CopyListUris;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class CompressParams implements Serializable {

    // direct input to compressArchive
    public BasePathContent srcDirectory;
    public BasePathContent destArchive;
    public Integer compressionLevel;
    public Boolean encryptHeaders;
    public Boolean solidMode;
    public String password;
    public List<String> filenames;

    public CopyListUris uris; // for content provider mode

    public boolean standaloneMode;

    public CompressParams(BasePathContent srcDirectory,
                          BasePathContent destArchive,
                          @Nullable Integer compressionLevel,
                          @Nullable Boolean encryptHeaders,
                          @Nullable Boolean solidMode,
                          @Nullable String password,
                          @Nullable List<String> filenames,
                          boolean standaloneMode) {
        this.srcDirectory = srcDirectory;
        this.destArchive = destArchive;
        this.compressionLevel = compressionLevel;
        this.encryptHeaders = encryptHeaders;
        this.solidMode = solidMode;
        this.password = password;
        this.filenames = filenames;
        this.standaloneMode = standaloneMode;
    }

    // constructor for content provider with uri list
    public CompressParams(CopyListUris uris,
                          BasePathContent destArchive,
                          @Nullable Integer compressionLevel,
                          @Nullable Boolean encryptHeaders,
                          @Nullable Boolean solidMode,
                          @Nullable String password,
                          boolean standaloneMode) {
        this.uris = uris;
        this.destArchive = destArchive;
        this.compressionLevel = compressionLevel;
        this.encryptHeaders = encryptHeaders;
        this.solidMode = solidMode;
        this.password = password;
        this.standaloneMode = standaloneMode;
    }
}
