package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.sftpclient.AuthData;

/**
 * Created by pgp on 13/05/17.
 *
 * LocalDirWithContent has no special attributes with respect to {@link GenericDirWithContent}
 */

public class SftpDirWithContent extends GenericDirWithContent {

    public AuthData authData; // user@domain:port, to display in address bar
    public String pendingLsPath;

    public SftpDirWithContent(AuthData authData, String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.SFTP;
        this.authData = authData;
    }

    public SftpDirWithContent(AuthData authData, FileOpsErrorCodes errorCode, String pendingLsPath) {
        super(errorCode);
        this.providerType = ProviderType.SFTP;
        this.authData = authData;
        this.pendingLsPath = pendingLsPath;
    }
}
