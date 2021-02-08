package it.pgp.xfiles.service.params;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class ExtractParams implements Serializable {

    // direct input to extractArchive
    public List<BasePathContent> srcArchives; // subDir taken from here
    public BasePathContent destDirectory;
    public String password;
    public Iterable<String> filenames;
    public boolean smartDirectoryCreation;

    public ExtractParams(List<BasePathContent> srcArchives,
                         @Nullable BasePathContent destDirectory, // null when testing archive
                         @Nullable String password,
                         @Nullable Iterable<String> filenames,
                         boolean smartDirectoryCreation) {
        this.srcArchives = srcArchives;
        this.destDirectory = destDirectory;
        this.password = password;
        this.filenames = filenames;
        this.smartDirectoryCreation = smartDirectoryCreation;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
