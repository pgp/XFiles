package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.smbclient.SmbAuthData;

public class SmbRemotePathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.SMB;

    public SmbAuthData smbAuthData; // user@domain:host:port, to display in address bar

    // empty constructor used by abstract factory
    public SmbRemotePathContent() {
        super(null,provType);
    }

    public SmbRemotePathContent(SmbAuthData smbAuthData, String dir) {
        super(dir,provType);
        this.smbAuthData = smbAuthData;
    }

    public SmbRemotePathContent(SmbAuthData smbAuthData, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.smbAuthData = smbAuthData;
    }

    @Override
    public String toString() { // non-canonical, contains domain(e.g. WORKGROUP)
        return "smb://"+smbAuthData+dir;
    }

    public String toConnString() { // canonical, can be fed as input into JCFIS SMbClient constructor
        return "smb://"+smbAuthData.username+"@"+
                smbAuthData.host+":"+smbAuthData.port+ // TODO check if access by custom port is canonical
                dir+"/";
    }

    @Override
    public BasePathContent concat(String filename) {
        String sep = dir.equals("/")?"":"/";
        return new SmbRemotePathContent(smbAuthData,dir+sep+filename);
    }

    @Override
    public BasePathContent getParent() {
        if (dir == null || dir.equals("/") || dir.equals("")) return null;
        int idx = dir.lastIndexOf('/');
        return new SmbRemotePathContent(smbAuthData, dir.substring(0,idx));
    }

    @Override
    public BasePathContent getCopy() {
        return new SmbRemotePathContent(smbAuthData,dir);
    }
}
