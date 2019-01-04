package it.pgp.xfiles.utils;

import java.io.File;

/**
 * Created by pgp on 01/10/16
 */

@Deprecated
public class DirWithContent {
    // TODO modifications for better decoupling control from view:
    // TODO DirWithContent should contain a list of BrowserItem, and not of files
    // TODO dir field should be a String, not a File object

    // TODO modifications above are being performed in DirWithContentUsingBrowserItems

    public File dir;
    public File[] content;
    public int errorCode; // 0 on success, >0 otherwise // TODO error constants table

    public DirWithContent(File dir, File[] content) {
        this.dir = dir;
        this.content = content;
    }
}
