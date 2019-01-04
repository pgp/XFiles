package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.KeyPair;

import net.schmizz.sshj.common.Base64;
import net.schmizz.sshj.common.Buffer;

import java.io.File;
import java.security.KeyFactory;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 04/12/17
 * TODO selector with types: RSA, ED25519
 * inflating dynamic layout containing selector
 *      RSA: 2048 - 4096 - custom
 *      ED25519: no options (ECC for public key auth unsupported in SSHJ)
 */

class SSHKeygenDialog extends Dialog {
    EditText name;
    String name_;
    private EditText bits;
    TextView wait;
    ProgressBar waitPb;
    Button ok;

    int bits_ = -1;
    final VaultActivity vaultActivity;
    final File destDir;

    File destPrv,destPub;

    SSHKeygenDialog(@NonNull VaultActivity vaultActivity) {
        super(vaultActivity);
        this.vaultActivity = vaultActivity;
        destDir = new File(vaultActivity.getFilesDir(),".ssh");
        setTitle("SSH RSA Keygen"); // RSA only, until SSHJ will support ECC for pubkey auth
        setContentView(R.layout.ssh_keygen_dialog);
        name = findViewById(R.id.sshKeygenNameEditText);
        bits = findViewById(R.id.sshKeygenBitsEditText);
        wait = findViewById(R.id.sshKeygenWaitTextView);
        waitPb = findViewById(R.id.sshKeygenWaitProgressBar);
        ok = findViewById(R.id.sshKeygenOkButton);
        ok.setOnClickListener(v -> {
            try { bits_ = Integer.valueOf(bits.getText().toString()); }
            catch (Exception ignored) {}
            // TODO replace edittext with selector (with "long time" warning on selecting 8192)
            if (bits_ != 2048 && bits_ != 3072 && bits_ != 4096 && bits_ != 8192) {
                Toast.makeText(vaultActivity, "Invalid bits, allowed: 2048, 3072, 4096, 8192", Toast.LENGTH_SHORT).show();
                return;
            }

            name_ = name.getText().toString();
            String filename = "id_rsa_"+name_;
            destPrv = new File(destDir,filename);
            destPub = new File(destDir,filename+".pub");
            if (destPrv.exists() || destPub.exists()) {
                Toast.makeText(vaultActivity, "At least a key with with the chosen name already exists, delete it before generating a new keypair", Toast.LENGTH_SHORT).show();
                return;
            }
            new KeygenTask(SSHKeygenDialog.this).execute();
        });
    }

    private static class KeygenTask extends AsyncTask {
        private final SSHKeygenDialog dialog;
        KeygenTask(SSHKeygenDialog dialog) {
            this.dialog = dialog;
        }

        FileOpsErrorCodes result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setCancelable(false);
            dialog.ok.setEnabled(false);
            dialog.wait.setText("Generating keys, please wait...");
            dialog.waitPb.setVisibility(View.VISIBLE);
        }

        // without JSCh, with Botan
        @Override
        protected Object doInBackground(Object[] unused) {
            try {
                String[] keys = MainActivity.getRootHelperClient().generatePEMKeyPair(dialog.bits_); // RSA only for now
                if (keys == null) {
                    result = FileOpsErrorCodes.CONNECTION_ERROR;
                    return null;
                }
                RSAKey[] keypair = PEMToJCEKeypair(keys[0],keys[1]);
                String sshPubKey = JCEPubKeyToSSHViaSSHJ((RSAPublicKey) keypair[1], dialog.name_+"@botan");
                String sshPrvKey = JCEPrvKeyToSSH((RSAPrivateKey) keypair[0]);

                if (!(Misc.writeStringToFilePath(sshPrvKey,dialog.destPrv.getAbsolutePath()) &&
                        Misc.writeStringToFilePath(sshPubKey,dialog.destPub.getAbsolutePath())))
                    result = FileOpsErrorCodes.TRANSFER_ERROR;
            }
            catch(Exception e){
                e.printStackTrace();
                result = FileOpsErrorCodes.CONNECTION_ERROR;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Object o) {
            if (result == null) {
                Toast.makeText(dialog.vaultActivity, "Keys generated successfully", Toast.LENGTH_SHORT).show();
                dialog.vaultActivity.idVaultAdapter.notifyDataSetChanged();
                MainActivity.mainActivity.goDir(new LocalPathContent(dialog.destDir.getAbsolutePath()));
            }
            else {
                Toast.makeText(dialog.vaultActivity, "Key generation error", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        }
    }

    /* Helper methods for converting PEM output to SSH compatible formats (id_rsa + id_rsa.pub) */

    private static String eolEveryNChars(String input, int n) {
        char[] x = input.toCharArray();
        StringBuilder sb = new StringBuilder();
        int q,r;
        q = x.length/n;
        r = x.length%n;
        for (int i=0;i<q;i++) {
            sb.append(x,i*n,n);
            sb.append('\n');
        }
        sb.append(x,q*n,r);
        return sb.toString();
    }

    // input from response to ACTION_SSH_KEYGEN request
    static RSAKey[] PEMToJCEKeypair(String privateKeyContent, String publicKeyContent) throws Exception {
        privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKeyContent));
        RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpecPKCS8);

        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.decode(publicKeyContent));
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);

        return new RSAKey[]{privKey,pubKey};
    }

    static String JCEPubKeyToSSHViaSSHJ(RSAPublicKey key, String comment) {
        return "ssh-rsa " + Base64.encodeBytes(
                new Buffer.PlainBuffer().putPublicKey(key).getCompactData()
        ) + " " + comment;
    }

    static String JCEPrvKeyToSSH(RSAPrivateKey key) {
        return "-----BEGIN RSA PRIVATE KEY-----\n"+
                eolEveryNChars(
                        Base64.encodeBytes(key.getEncoded()),64
                ) + "\n-----END RSA PRIVATE KEY-----\n";
    }

}
