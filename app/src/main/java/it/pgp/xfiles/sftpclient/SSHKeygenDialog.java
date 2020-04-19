package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
            if (bits_ != 2048 && bits_ != 3072 && bits_ != 4096 && bits_ != 6144 && bits_ != 8192) {
                Toast.makeText(vaultActivity, "Invalid bits, allowed: 2048, 3072, 4096, 6144, 8192", Toast.LENGTH_SHORT).show();
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
                String sshPrvKey = JCEPrvKeyToPKCS1((RSAPrivateKey) keypair[0]);

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
                MainActivity.mainActivity.goDir(
                        new LocalPathContent(dialog.destDir.getAbsolutePath()),
                        MainActivity.mainActivity.browserPager.getCurrentItem(),
                        null);
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

    /**
     * SSHJ, differently from OpenSSH, seems to have trouble with PKCS8-encoded PEM private keys
     * (the ones that are generated by Botan for performance reasons), so we convert them to
     * "traditional" PKCS1 format
     * Web source:
     * https://stackoverflow.com/questions/7611383/generating-rsa-keys-in-pkcs1-format-in-java
     */
    static String JCEPrvKeyToPKCS1(RSAPrivateKey priv) throws IOException {
        // assemble PKCS1 content
        byte[] privBytes = priv.getEncoded();
        PrivateKeyInfo pkInfo = PrivateKeyInfo.getInstance(privBytes);
        ASN1Encodable encodable = pkInfo.parsePrivateKey();
        ASN1Primitive primitive = encodable.toASN1Primitive();
        byte[] privateKeyPKCS1 = primitive.getEncoded();

        // export in PEM format
        PemObject pemObject = new PemObject("RSA PRIVATE KEY", privateKeyPKCS1);
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        return stringWriter.toString();
    }

}
