package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.utils.pathcontent.XREPathContent;

/**
 * Created by pgp on 20/09/17
 */

public class XFilesRemoteDirWithContent extends GenericDirWithContent {

    public static final int defaultRHRemoteServerPort = XREPathContent.defaultRHRemoteServerPort;

    public String serverHost;
    public int serverPort;

    public XFilesRemoteDirWithContent(String serverHost, String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.XFILES_REMOTE;
        this.serverHost = serverHost;
        this.serverPort = defaultRHRemoteServerPort;
    }

    public XFilesRemoteDirWithContent(String serverHost, int serverPort, String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.XFILES_REMOTE;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public XFilesRemoteDirWithContent(String serverHost, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.providerType = ProviderType.XFILES_REMOTE;
        this.serverHost = serverHost;
        this.serverPort = defaultRHRemoteServerPort;
    }
}
