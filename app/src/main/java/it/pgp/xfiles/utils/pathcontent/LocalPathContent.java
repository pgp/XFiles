package it.pgp.xfiles.utils.pathcontent;

import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 13/05/17
 */

public class LocalPathContent extends BasePathContent {

    private static final ProviderType provType = ProviderType.LOCAL;

    // empty constructor used by abstract factory
    public LocalPathContent() {
        super(null,provType);
    }

    public LocalPathContent(String dir) {
        super(dir,provType);
    }

    public LocalPathContent(FileOpsErrorCodes errorCode) {
        super(provType, errorCode);
    }

    @Override
    public String toString() {
        return dir;
    }

    @Override
    public BasePathContent concat(String filename) {
        String sep = dir.equals("/")?"":"/";
        return new LocalPathContent(dir+sep+filename);
    }

    @Override
    public BasePathContent getParent() {
        if (dir.equals("/")) return null;
        int idx = dir.lastIndexOf('/');
        return new LocalPathContent(dir.substring(0,idx));
    }

    @Override
    public BasePathContent getCopy() {
        return new LocalPathContent(dir);
    }
}
