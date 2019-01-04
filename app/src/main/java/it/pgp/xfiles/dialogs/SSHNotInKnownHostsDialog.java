package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.schmizz.sshj.common.KeyType;

import java.io.IOException;
import java.security.PublicKey;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.sftpclient.SFTPProviderUsingPathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

/**
 * Created by pgp on 03/03/17
 *
 * Dialog to be displayed when a host key does not exist in known hosts:
 *     - asking for adding it
 *     - showing SHA256 fingerprint of the host key
 */

public class SSHNotInKnownHostsDialog extends Dialog {

    public RemotePathContent pendingLsPath;
    public void resetPath() {
        pendingLsPath = null;
    }

    public SSHNotInKnownHostsDialog(final Context context,
                                    final AuthData authData,
                                    final PublicKey hostKey,
                                    final SFTPProviderUsingPathContent provider,
                                    final BasePathContent pendingLsPath) {
        super(context);
        this.pendingLsPath = (RemotePathContent) pendingLsPath;
        setOnDismissListener(MainActivity.sftpRetryLsListener);

        setTitle("Unknown host key");
        setContentView(R.layout.ssh_not_in_known_hosts_dialog);
        TextView fingerprint = findViewById(R.id.hostKeyFingerprintTextView);
        Button accept = findViewById(R.id.hostKeyAcceptButton);
        Button discard = findViewById(R.id.hostKeyDiscardButton);

        fingerprint.setText(KeyType.fromKey(hostKey)+" "+hostKey.getAlgorithm()+" "+hostKey.getFormat());

        accept.setOnClickListener(v -> {
            try {
                provider.addHostKey(authData.domain,hostKey);
                Toast.makeText(context,"Host key added to known hosts",Toast.LENGTH_LONG).show();
                dismiss();
                // retry getChannel and LS pending request (if any) is done in dismiss listener
            } catch (IOException e) {
                Toast.makeText(context,"Unable to add host key to known hosts",Toast.LENGTH_LONG).show();
                resetPath();
                cancel();
            }
        });

        discard.setOnClickListener(v -> {
            resetPath();
            cancel();
        });

    }
}
