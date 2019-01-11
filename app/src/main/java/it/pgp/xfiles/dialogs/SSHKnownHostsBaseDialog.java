package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

public abstract class SSHKnownHostsBaseDialog extends Dialog {

    public RemotePathContent pendingLsPath;
    public void resetPath() {
        pendingLsPath = null;
    }

    public SSHKnownHostsBaseDialog(Context context, BasePathContent pendingLsPath) {
        super(context);
        this.pendingLsPath = (RemotePathContent) pendingLsPath;
        setOnDismissListener(MainActivity.sftpRetryLsListener);
    }
}
