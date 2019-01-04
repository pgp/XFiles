package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 13/05/17
 */

public class ArchivePathContent extends BasePathContent {
    public String archivePath;
    public String password; // to list/extract from password-protected archives

    private static final ProviderType provType = ProviderType.LOCAL_WITHIN_ARCHIVE;

    // empty constructor used by abstract factory
    public ArchivePathContent() {
        super(null,provType);
    }

    // archivePath: archive absolute pathname in the local filesystem
    // dir: directory path relative to the archive root
    public ArchivePathContent(String archivePath, String dir) {
        super(dir,provType);
        this.archivePath = archivePath;
    }

    public ArchivePathContent(String archivePath, String dir, String password) {
        super(dir,provType);
        this.archivePath = archivePath;
        this.password = password;
    }

    public ArchivePathContent(String archivePath, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.archivePath = archivePath;
    }

    @Override
    public String toString() {
        if (dir.equals("") || dir.equals("/"))
            return archivePath;
        return archivePath+"/"+dir;
    }

    @Override
    public BasePathContent concat(String filename) {
        String s;
        if (dir == null || dir.equals("/") || dir.equals("")) s = filename;
        else s = dir+"/"+filename;
        return new ArchivePathContent(archivePath,s);
    }

    @Override
    public BasePathContent getParent() {
        // exit from archive, change subclass type
        if (dir == null || dir.equals("/") || dir.equals(""))
            return new LocalPathContent(archivePath).getParent();
        else {
            int idx = dir.lastIndexOf('/');
            if (idx < 0) return new ArchivePathContent(archivePath,"");
            return new ArchivePathContent(this.archivePath,dir.substring(0,idx));
        }
    }

    @Override
    public BasePathContent getCopy() {
        return new ArchivePathContent(archivePath,dir,password);
    }
}
