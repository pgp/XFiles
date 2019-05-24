package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 13/05/17
 */

public class ArchiveSubDirWithContent extends GenericDirWithContent {

    public String archivePath; // archive absolute pathname in the local filesystem

    public ArchiveSubDirWithContent(String archivePath, String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.LOCAL_WITHIN_ARCHIVE;
        this.archivePath = archivePath;
    }

    public ArchiveSubDirWithContent(String archivePath, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.providerType = ProviderType.LOCAL_WITHIN_ARCHIVE;
        this.archivePath = archivePath;
    }
}
