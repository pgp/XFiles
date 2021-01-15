package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.sftpclient.AuthData;

/**
 * Created by pgp on 13/05/17
 */

public class SFTPPathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.SFTP;

    public AuthData authData; // user@domain:port, to display in address bar

    public SFTPPathContent(AuthData authData, String dir) {
        super(provType, dir);
        this.authData = authData;
    }

    public SFTPPathContent(AuthData authData, FileOpsErrorCodes errorCode) {
        super(provType, errorCode);
        this.authData = authData;
    }

    @Override
    public String toString() {
        return "sftp://"+authData+dir;
    }

    @Override
    public BasePathContent concat(String filename) {
        String sep = dir.equals("/")?"":"/";
        return new SFTPPathContent(authData,dir+sep+filename);
    }

    @Override
    public BasePathContent getParent() {
        if (dir == null || dir.equals("/") || dir.equals("")) return null;
        int idx = dir.lastIndexOf('/');
        return new SFTPPathContent(authData, dir.substring(0,idx));
    }

    @Override
    public BasePathContent getCopy() {
        return new SFTPPathContent(authData,dir);
    }
}
