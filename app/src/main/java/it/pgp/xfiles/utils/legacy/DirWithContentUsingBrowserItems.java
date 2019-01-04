package it.pgp.xfiles.utils.legacy;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;

/**
 * Created by pgp on 26/01/17
 */

@Deprecated
public class DirWithContentUsingBrowserItems {
    public String dir;
    public List<BrowserItem> content;
    public FileOpsErrorCodes errorCode; // null on success, errno-equivalent or descriptive commander error otherwise
    public Integer listViewPosition;

    public DirWithContentUsingBrowserItems(String dir, List<BrowserItem> content) {
        this.dir = dir;
        this.content = content;
        this.listViewPosition = 0;
    }

    public DirWithContentUsingBrowserItems(FileOpsErrorCodes errorCode) {
        this.errorCode = errorCode;
    }
}
