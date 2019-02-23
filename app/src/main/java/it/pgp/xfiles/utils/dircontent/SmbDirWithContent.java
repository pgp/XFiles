package it.pgp.xfiles.utils.dircontent;

import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.smbclient.SmbAuthData;

public class SmbDirWithContent extends GenericDirWithContent {
    public SmbAuthData smbAuthData; // user@domain:host:port, to display in address bar

    public SmbDirWithContent(SmbAuthData smbAuthData, String dir, List<BrowserItem> content) {
        super(dir, content);
        this.providerType = ProviderType.SMB;
        this.smbAuthData = smbAuthData;
    }

    public SmbDirWithContent(SmbAuthData smbAuthData, FileOpsErrorCodes errorCode) {
        super(errorCode);
        this.providerType = ProviderType.SMB;
        this.smbAuthData = smbAuthData;
    }
}
