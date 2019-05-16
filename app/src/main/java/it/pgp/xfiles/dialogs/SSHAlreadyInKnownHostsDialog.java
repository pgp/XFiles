package it.pgp.xfiles.dialogs;

import android.content.Context;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.schmizz.sshj.common.KeyType;

import java.io.IOException;
import java.security.PublicKey;

import it.pgp.xfiles.R;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.sftpclient.SFTPProviderUsingPathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 03/03/17
 *
 * Dialog to be displayed when a host key does not match the one
 * already in known hosts. Choices are:
 *    - overwrite old host key and continue connecting
 *    - keep old host key and abort connection
 *    - (not really necessary: temporarily accept new host key, but don't remove old one)
 */

public class SSHAlreadyInKnownHostsDialog extends SSHKnownHostsBaseDialog {

    public SSHAlreadyInKnownHostsDialog(final Context context,
                                        final AuthData authData,
                                        final PublicKey oldHostKey,
                                        final PublicKey newHostKey,
                                        final SFTPProviderUsingPathContent provider,
                                        final BasePathContent pendingLsPath) {
        super(context,pendingLsPath);

        setTitle("Conflicting host key");
        setContentView(R.layout.ssh_already_in_known_hosts_dialog);
        TextView oldFingerprint = findViewById(R.id.storedHostKeyFingerprintTextView);
        TextView newFingerprint = findViewById(R.id.currentHostKeyFingerprintTextView);
        Button accept = findViewById(R.id.hostKeyAcceptOverwriteButton);
        Button discard = findViewById(R.id.hostKeyKeepOldAndDisconnectButton);

        oldFingerprint.setText(KeyType.fromKey(oldHostKey)+" "+
                oldHostKey.getAlgorithm()+" "+
                oldHostKey.getFormat());

        newFingerprint.setText(KeyType.fromKey(newHostKey)+" "+
                newHostKey.getAlgorithm()+" "+
                newHostKey.getFormat());

        accept.setOnClickListener(v -> {
            try {
                final String adjustedHostname = (authData.port != 22) ? "[" + authData.domain + "]:" + authData.port : authData.domain;
                provider.updateHostKey(adjustedHostname,newHostKey);
                Toast.makeText(context,"Host key updated in known hosts",Toast.LENGTH_LONG).show();
                dismiss();
                // retry getChannel and LS pending request (if any) is done in dismiss listener
            } catch (IOException e) {
                Toast.makeText(context,"Unable to update host key in known hosts",Toast.LENGTH_LONG).show();
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
