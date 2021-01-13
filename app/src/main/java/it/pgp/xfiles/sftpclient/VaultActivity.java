package it.pgp.xfiles.sftpclient;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.FileSelectFragment;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.XFilesUtilsUsingPathContent;

/**
 * Created by pgp on 11/02/17 (adapted from KeyGuard)
 */

public class VaultActivity extends EffectActivity implements FileSelectFragment.Callbacks {

    public static Map<Long,?> vault;
    public GenericDBHelper dbh;

    public ListView vaultListView;
    public ListView idVaultListView;

    public VaultAdapter vaultAdapter;
    public IdentitiesVaultAdapter idVaultAdapter;

    public Button addNewItemBtn, addNewIdBtn, genNewIdBtn;

    public void openFileSelector() {
        String fragTag = getResources().getString(R.string.tag_fragment_FileSelect);

        // Set up a selector for file selection rather than directory selection.
        FileSelectFragment fsf = FileSelectFragment.newInstance(FileSelectFragment.Mode.FileSelector,
                android.R.string.ok,
                android.R.string.cancel,
                R.string.alert_file_select,
                R.drawable.xfiles_new_app_icon,
                R.drawable.xf_dir_blu,
                R.drawable.xfiles_file_icon);

        fsf.show(getFragmentManager(), fragTag);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SFTP Key Manager");
        setActivityIcon(R.drawable.xf_xre_server_up);
        setContentView(R.layout.sftp_vault_list_layout);
        dbh = new GenericDBHelper(getApplicationContext());
        vaultListView= findViewById(R.id.sftp_passitem_List);
        idVaultListView= findViewById(R.id.sftp_ids_List);
        addNewItemBtn= findViewById(R.id.sftpAddNewCredsBtn);
        addNewIdBtn = findViewById(R.id.sftpAddNewIdentityBtn);
        genNewIdBtn = findViewById(R.id.sftpGenNewIdentityBtn);

        addNewItemBtn.setOnClickListener(v -> new InsertEditDialog(VaultActivity.this,vaultAdapter).show());

        addNewIdBtn.setOnClickListener(v -> openFileSelector());
        genNewIdBtn.setOnClickListener(v -> new SSHKeygenDialog(VaultActivity.this).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        vault = dbh.getAllCreds(AuthData.ref);
        vaultAdapter = new VaultAdapter(getApplicationContext(),VaultActivity.this,vault);
        vaultListView.setAdapter(vaultAdapter);
        idVaultAdapter = new IdentitiesVaultAdapter(this);
        idVaultListView.setAdapter(idVaultAdapter);
    }

    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        if (absolutePath != null && fileName != null) {
            File inputPrivKey = new File(absolutePath,fileName);
            File destPath = new File(getApplicationContext().getFilesDir(), SFTPProviderUsingPathContent.sshIdsDirName);
            if (!destPath.exists()) destPath.mkdirs();
            destPath = new File(destPath,fileName);

            // check private key format before copying
            try {
                KeyProvider kprov = new SSHClient().loadKeys(inputPrivKey.getAbsolutePath());
                switch (kprov.getType()) {
                    case RSA:
                    case ED25519:
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),"Only RSA and ED25519 keys allowed",Toast.LENGTH_SHORT).show();
                        return;
                }
            }
            catch (Exception i) { // wrong key format or read error
                Toast.makeText(getApplicationContext(),"Wrong key format or key read error",Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO in case of already existing filename, show alert dialog and ask what to do (overwrite, rename, cancel), for now simply cancel key import
            if (destPath.exists()) {
                Toast.makeText(getApplicationContext(),"A key file with the same name already exists, remove it before adding this one",Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                XFilesUtilsUsingPathContent.copyFile(inputPrivKey,destPath);
            }
            catch (IOException e) {
                Toast.makeText(getApplicationContext(),"Key import error",Toast.LENGTH_SHORT).show();
                return;
            }

            idVaultAdapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(),"Key imported successfully",Toast.LENGTH_SHORT).show();
            showRefreshClientDialog();
        }
    }

    @Override
    public boolean isValid(String absolutePath, String fileName) {
        return true;
    }

    // FIXME causes crash on Android-x86 VM
    // ask to refresh sftp clients (clear map) after identity add (otherwise public-key auth only attempts will be successful only on not-already connected sftp clients)
    public void showRefreshClientDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Reset connected clients in order to update private key ids?");
        b.setIcon(R.drawable.xf_xre_server_down);
        b.setNegativeButton(android.R.string.cancel, null);
        b.setPositiveButton(android.R.string.ok, (dialog,which)->MainActivity.sftpProvider.closeAllSessions());
        b.create().show();
    }
}
