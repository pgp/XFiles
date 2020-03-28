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
    public BasePathContent srcArchive; // subDir taken from here
    public BasePathContent destDirectory;
    public String password;
    public List<String> filenames;
    public boolean smartDirectoryCreation;

    public ExtractParams(BasePathContent srcArchive,
                         BasePathContent destDirectory,
                         @Nullable String password,
                         @Nullable List<String> filenames,
                         boolean smartDirectoryCreation) {
        this.srcArchive = srcArchive;
        this.destDirectory = destDirectory;
        this.password = password;
        this.filenames = filenames;
        this.smartDirectoryCreation = smartDirectoryCreation;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
