package it.pgp.xfiles.utils.pathcontent;

import java.net.MalformedURLException;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.smbclient.SmbAuthData;
import jcifs.CIFSContext;
import jcifs.smb.SmbFile;

public class SmbRemotePathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.SMB;

    public SmbAuthData smbAuthData; // user@domain:host:port, to display in address bar

    public SmbRemotePathContent(SmbAuthData smbAuthData, String dir) {
        super(dir,provType);
        this.smbAuthData = smbAuthData;
    }

    public SmbRemotePathContent(SmbAuthData smbAuthData, FileOpsErrorCodes errorCode) {
        super(provType, errorCode);
        this.smbAuthData = smbAuthData;
    }

    @Override
    public String toString() { // non-canonical, contains domain(e.g. WORKGROUP)
        return "smb://"+smbAuthData+dir;
    }

    public String toConnStringFull() { // canonical, can be fed as input into JCFIS SMbClient constructor
        return "smb://"+smbAuthData.username+"@"+
                smbAuthData.host+":"+smbAuthData.port+ // TODO check if access by custom port is canonical
                dir+"/";
    }

    public String toConnString(boolean isDirectory) { // TODO check if port in address is canonical
        return "smb://"+smbAuthData.host+":"+smbAuthData.port+dir+(isDirectory?"/":"");
    }

    public SmbFile getSmbFile(CIFSContext context, boolean isDirectory) throws MalformedURLException {
        return new SmbFile(toConnString(isDirectory),context);
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
