package it.pgp.xfiles.enums;

import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;

public enum ProviderType {
    LOCAL(LocalPathContent::new),
    LOCAL_WITHIN_ARCHIVE(ArchivePathContent::new),
    SFTP(RemotePathContent::new),
    XFILES_REMOTE(XFilesRemotePathContent::new),
    URL_DOWNLOAD(null);

    interface CreatePathInterface {
        BasePathContent create();
    }

    final CreatePathInterface cpi;

    ProviderType(CreatePathInterface cpi) {
        this.cpi = cpi;
    }

    public BasePathContent create() {
        return cpi.create();
    }
}
