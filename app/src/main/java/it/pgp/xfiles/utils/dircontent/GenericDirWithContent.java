package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 13/05/17
 *
 * represent the response to UI activities, that is, a response to be visualized through an ArrayAdapter or similar
 * the content here must represent ONE level in a filesystem hierarchy (only children of the folder,
 * NOT all the archive entries in case of archive - the VMap caching must be done at FileOpsHelper/RootHelperClient level)
 */

public class GenericDirWithContent {
    public ProviderType providerType;
    /*
    content of dir for providerType:
        - LOCAL: absolute path of the directory in the filesystem
        - LOCAL_WITHIN_ARCHIVE: absolute path of the compressed directory in the archive defined in the subclass
        - REMOTE: absolute path of the directory in the remote filesystem
     */
    public String dir;

    public List<BrowserItem> content; // each BrowserItem contains a filename
    public FileOpsErrorCodes errorCode; // null on success, errno-equivalent or descriptive commander error otherwise
    public Integer listViewPosition;

    public GenericDirWithContent(String dir, List<BrowserItem> content) {
        this.dir = dir;
        this.content = content;
        this.listViewPosition = 0;
    }


    /* TODO
     * in all subclasses errorCode constructors (or equivalently in this one first)
     * add dir as input parameter, to have complete information about error
     */
    public GenericDirWithContent(FileOpsErrorCodes errorCode) {
        this.errorCode = errorCode;
    }
}
