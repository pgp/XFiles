package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 20/09/17
 */

public class XFilesRemotePathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.XFILES_REMOTE;

    public static final int defaultRHRemoteServerPort = 11111;

    public String serverHost;
    int serverPort;

    // empty constructor used by abstract factory
    public XFilesRemotePathContent() {
        super(null,provType);
    }

    public XFilesRemotePathContent(String serverHost, String dir) {
        super(dir,provType);
        this.serverHost = serverHost;
        this.serverPort = defaultRHRemoteServerPort;
    }

    public XFilesRemotePathContent(String serverHost, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.serverHost = serverHost;
        this.serverPort = defaultRHRemoteServerPort;
    }

    @Override
    public String toString() {
        return "xre://"+serverHost+":"+serverPort+dir;
    }

    @Override
    public BasePathContent concat(String filename) {
        String sep = dir.equals("/")?"":"/";
        return new XFilesRemotePathContent(serverHost,dir+sep+filename);
    }

    @Override
    public BasePathContent getParent() {
        if (dir==null || dir.equals("/") || dir.equals("")) return null;
        int idx = dir.lastIndexOf('/');
        return new XFilesRemotePathContent(serverHost,dir.substring(0,idx));
    }

    @Override
    public BasePathContent getCopy() {
        return new XFilesRemotePathContent(serverHost,dir);
    }
}
