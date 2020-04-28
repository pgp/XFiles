package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.security.PublicKey;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

public abstract class SSHKnownHostsBaseDialog extends Dialog {

    public static String getHostkeyFingerprint(Object hostKey) {
        try {
            return (hostKey instanceof OpenSSHKnownHosts.KnownHostEntry)?
                    ((OpenSSHKnownHosts.KnownHostEntry) hostKey).getFingerprint()
                    :SecurityUtils.getFingerprint((PublicKey) hostKey);
        }
        catch(Exception e) {
            e.printStackTrace();
            return KeyType.UNKNOWN.name();
        }
    }

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
