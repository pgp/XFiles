package it.pgp.xfiles.utils.pathcontent;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.smbclient.SmbAuthData;

import static it.pgp.Native.nHashCode;

/**
 * Created by pgp on 13/05/17
 */

public abstract class BasePathContent implements Serializable {

    public final ProviderType providerType;
    /*
    content of dir for providerType:
        - LOCAL: absolute path of the directory in the filesystem
        - LOCAL_WITHIN_ARCHIVE: absolute path of the compressed directory in the archive defined in the subclass
        - SFTP: absolute path of the directory in the remote filesystem
        - XFILES_REMOTE: absolute path of the directory in the remote filesystem
        - SMB: absolute path of the directory in the remote filesystem
     */
    public String dir;

    public FileOpsErrorCodes errorCode; // null on success, errno-equivalent or descriptive commander error otherwise

    public BasePathContent(String dir, ProviderType providerType) {
        this.providerType = providerType;
        // not the best solution (embed subclass logic in the parent class),
        // just to avoid duplicating this check in all subclasses
        if (providerType != ProviderType.XFILES_REMOTE && (dir == null || dir.equals(""))) this.dir = "/";
        else {
            this.dir = dir;
//            if(this.dir.endsWith("/") && !this.dir.equals("/"))
//                this.dir = this.dir.substring(0,this.dir.length()-1);
        }
    }

    public BasePathContent(FileOpsErrorCodes errorCode) {
        this.providerType = null;
        this.errorCode = errorCode;
    }

    public abstract String toString();

    public abstract BasePathContent concat(String filename);

    public abstract BasePathContent getParent();

//    public static boolean isParentOrSamePathNoDuplicateSlashes(String currentDir, String targetDir) {
//        String[] p1 = currentDir.split("/");
//        if(p1.length == 0) return false;
//        String[] p2 = targetDir.split("/");
//        if(p2.length == 0) return false;
//        if(p1.length > p2.length) return false;
//        for(int i=0;i<p1.length;i++) {
//            if(!p1[i].equals(p2[i])) return false;
//        }
//        return true;
//    }

    // FIXME precondition: paths both starting with "/" -> conflicts with dir member in ArchivePathContent
    public static boolean isParentOrSamePath(String currentDir, String targetDir) {
//        if(!currentDir.startsWith("/") || !targetDir.startsWith("/"))
//            throw new RuntimeException("Paths must start with \"/\"");
        String[] p1 = currentDir.split("/");
        String[] p2 = targetDir.split("/");
        List<String> l1 = new ArrayList<>();
        List<String> l2 = new ArrayList<>();
        for(String s : p1) if(!s.isEmpty()) l1.add(s);
        for(String s : p2) if(!s.isEmpty()) l2.add(s);
        int l1s = l1.size();
        int l2s = l2.size();
        if(l1s == 0) return true; // "/" is parent of everyone, at worst it's the same with itself
        if(l1s > l2s) return false;
        for(int i=0;i<l1.size();i++) {
            if(!l1.get(i).equals(l2.get(i))) return false;
        }
        return true;
    }

    // method to avoid copy of parent path into child path (which could cause path overflow or filesystem saturation)
    public boolean isParentOrSameOf(BasePathContent content) {
        if (this.providerType == ProviderType.LOCAL && content.providerType == ProviderType.LOCAL)
            return isParentOrSamePath(this.dir, content.dir);
        if (this.providerType == ProviderType.LOCAL && content.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE)
            return isParentOrSamePath(this.dir, ((ArchivePathContent)content).archivePath);
        if (this.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE
                && content.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE)
            return ((ArchivePathContent)content).archivePath.equals(((ArchivePathContent)this).archivePath)
            && content.dir.startsWith(this.dir);

        if (this.providerType==ProviderType.SFTP && content.providerType==ProviderType.SFTP)
            return ((RemotePathContent)content).authData.equals(((RemotePathContent)this).authData)
            && isParentOrSamePath(this.dir, content.dir);

        if (this.providerType==ProviderType.XFILES_REMOTE && content.providerType==ProviderType.XFILES_REMOTE)
            return ((XFilesRemotePathContent)content).serverHost.equals(((XFilesRemotePathContent)this).serverHost) &&
                    ((XFilesRemotePathContent)content).serverPort == ((XFilesRemotePathContent)this).serverPort &&
                    isParentOrSamePath(this.dir, content.dir);

        if (this.providerType==ProviderType.SMB && content.providerType==ProviderType.SMB)
            return ((SmbRemotePathContent)content).smbAuthData.equals(((SmbRemotePathContent)this).smbAuthData)
                    && isParentOrSamePath(this.dir, content.dir);

        return false;
    }

    public String getName() {
        if (dir.equals("/")) return dir;
        return dir.substring(dir.lastIndexOf('/')+1);
    }

    @Override
    public boolean equals(Object obj_) {
        if (!(obj_ instanceof BasePathContent)) return false;
        BasePathContent obj = (BasePathContent)obj_;
        if (this.providerType != obj.providerType) return false;
        if (this.errorCode != obj.errorCode) return false;
        switch (providerType) {
            case LOCAL_WITHIN_ARCHIVE:
                ArchivePathContent lwa = (ArchivePathContent)this;
                ArchivePathContent lwaobj = (ArchivePathContent)obj;
                if (!lwa.archivePath.equals(lwaobj.archivePath)) return false;
                break;
            case SFTP:
                RemotePathContent sftpc = (RemotePathContent)this;
                RemotePathContent sftpobj = (RemotePathContent)obj;
                if (!sftpc.authData.equals(sftpobj.authData)) return false;
                break;
            case XFILES_REMOTE:
                XFilesRemotePathContent xre = (XFilesRemotePathContent)this;
                XFilesRemotePathContent xreobj = (XFilesRemotePathContent)obj;
                if ((!xre.serverHost.equals(xreobj.serverHost)) ||
                        (xre.serverPort != xreobj.serverPort))
                    return false;
                break;
            case SMB:
                SmbRemotePathContent smbc = (SmbRemotePathContent) this;
                SmbRemotePathContent smbobj = (SmbRemotePathContent) obj;
                if (!smbc.smbAuthData.equals(smbobj.smbAuthData)) return false;
                break;
        }
        return dir.equals(obj.dir);
    }

    // does not keep error code field into account (assumes invalid path contents are not stored to be unique)
    @Override
    public int hashCode() {
        ByteBuffer bb;
        byte[] d = dir.getBytes(); // common field
        switch(providerType) {
            case LOCAL:
                return nHashCode(d);
            case LOCAL_WITHIN_ARCHIVE:
                byte[] ap = ((ArchivePathContent)this).archivePath.getBytes();
                bb = ByteBuffer.allocate(d.length+ap.length);
                bb.put(d);
                bb.put(ap);
                return nHashCode(bb.array());
            case SFTP:
                AuthData ad = ((RemotePathContent)this).authData;
                // it is fine to have same hash code for null fields and empty string fields
                byte[] u = (ad.username==null)?new byte[0]:ad.username.getBytes();
                byte[] dm = (ad.domain==null)?new byte[0]:ad.domain.getBytes();
                // ignore password, store on db without it (stored in sftpCredentials) so ignore in hashcode
                // since for connection, sftpCredentials table is queried anyway
                bb = ByteBuffer.allocate(u.length+dm.length+4+d.length);
                bb.put(u);bb.put(dm);bb.putInt(ad.port);bb.put(d);
                return nHashCode(bb.array());
            case XFILES_REMOTE:
                XFilesRemotePathContent xpc = (XFilesRemotePathContent)this;
                byte[] h = (xpc.serverHost==null)?new byte[0]:xpc.serverHost.getBytes();
                bb = ByteBuffer.allocate(h.length+4+d.length);
                bb.put(h);bb.putInt(xpc.serverPort);bb.put(d);
                return nHashCode(bb.array());
            case SMB:
                SmbAuthData smb_ad = ((SmbRemotePathContent)this).smbAuthData;
                // it is fine to have same hash code for null fields and empty string fields
                u = (smb_ad.username==null)?new byte[0]:smb_ad.username.getBytes();
                dm = (smb_ad.domain==null)?new byte[0]:smb_ad.domain.getBytes();
                h = (smb_ad.host==null)?new byte[0]:smb_ad.host.getBytes();
                // ignore password, store on db without it (stored in smbCredentials) so ignore in hashcode
                // since for connection, smbCredentials table is queried anyway
                bb = ByteBuffer.allocate(u.length+dm.length+h.length+4+d.length);
                bb.put(u);bb.put(dm);bb.put(h);bb.putInt(smb_ad.port);bb.put(d);
                return nHashCode(bb.array());
            default:
                throw new RuntimeException("Unexpected path content type");
        }
    }

    public abstract BasePathContent getCopy();

}
