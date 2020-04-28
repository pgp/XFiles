package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.File;
import java.security.PublicKey;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.SSHKnownHostsBaseDialog;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class SSHKeyInfoDialog extends Dialog {

    private final File idsDir;
    private final String idFilename;
    private final VaultActivity vaultActivity;

    SSHKeyInfoDialog(@NonNull VaultActivity vaultActivity, File idsDir, String idFilename) {
        super(vaultActivity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.vaultActivity = vaultActivity;
        this.idsDir = idsDir;
        this.idFilename = idFilename;
        setContentView(R.layout.ssh_key_info_dialog);

        try {
            KeyProvider kprov = new SSHClient().loadKeys(new File(idsDir, idFilename).getAbsolutePath());
            PublicKey key = kprov.getPublic();
            ((TextView)(findViewById(R.id.sshKeyInfoTypeTextView))).setText(
                    KeyType.fromKey(key)+" "+key.getAlgorithm()+" "+key.getFormat());
            ((TextView)(findViewById(R.id.sshKeyInfoFingerprintTextView))).setText(
                    SSHKnownHostsBaseDialog.getHostkeyFingerprint(key)
            );
            findViewById(R.id.sshKeyInfoLocateButton).setOnClickListener(this::locateKey);
        }
        catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(vaultActivity, "Unable to retrieve key information", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void locateKey(View unused) {
        MainActivity.mainActivity.goDir(
                new LocalPathContent(idsDir.getAbsolutePath()),
                MainActivity.mainActivity.browserPager.getCurrentItem(),
                idFilename
        );
        vaultActivity.finish();
    }
}
