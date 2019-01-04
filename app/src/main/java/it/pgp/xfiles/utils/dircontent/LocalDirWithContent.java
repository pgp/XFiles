package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 13/05/17.
 *
 * LocalDirWithContent has no special attributes with respect to {@link GenericDirWithContent}
 */

public class LocalDirWithContent extends GenericDirWithContent {

    public LocalDirWithContent(String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.LOCAL;
    }

    public LocalDirWithContent(FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.providerType = ProviderType.LOCAL;
    }
}
