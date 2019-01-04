package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.sftpclient.AuthData;

/**
 * Created by pgp on 13/05/17
 */

// TODO to be refactor-renamed to SFTPPathContent
public class RemotePathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.SFTP;

    public AuthData authData; // user@domain:port, to display in address bar

    // empty constructor used by abstract factory
    public RemotePathContent() {
        super(null,provType);
    }

    public RemotePathContent(AuthData authData, String dir) {
        super(dir,provType);
        this.authData = authData;
    }

    public RemotePathContent(AuthData authData, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.authData = authData;
    }

    @Override
    public String toString() {
        return "sftp://"+authData+dir;
    }

    @Override
    public BasePathContent concat(String filename) {
        String sep = dir.equals("/")?"":"/";
        return new RemotePathContent(authData,dir+sep+filename);
    }

    @Override
    public BasePathContent getParent() {
        if (dir == null || dir.equals("/") || dir.equals("")) return null;
        int idx = dir.lastIndexOf('/');
        return new RemotePathContent(authData, dir.substring(0,idx));
    }

    @Override
    public BasePathContent getCopy() {
        return new RemotePathContent(authData,dir);
    }
}
