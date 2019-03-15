package it.pgp.xfiles.smbclient;

import android.os.Bundle;

import it.pgp.xfiles.R;
import it.pgp.xfiles.sftpclient.InsertEditDialog;
import it.pgp.xfiles.sftpclient.VaultActivity;
import it.pgp.xfiles.utils.GenericDBHelper;

public class SmbVaultActivity extends VaultActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SMB Key Manager");
        setActivityIcon(R.drawable.xf_xre_server_up);
        setContentView(R.layout.smb_vault_list_layout);
        dbh = new GenericDBHelper(getApplicationContext());
        vaultListView= findViewById(R.id.smb_passitem_List);
        addNewItemBtn= findViewById(R.id.smbAddNewCredsBtn);
        addNewItemBtn.setOnClickListener(
                view -> {
                    insertEditDialog = new InsertEditDialog(SmbVaultActivity.this,vaultAdapter);
                    insertEditDialog.show();
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        vault = dbh.getAllCreds(SmbAuthData.ref);
        vaultAdapter = new SmbVaultAdapter(getApplicationContext(),SmbVaultActivity.this,vault);
        vaultListView.setAdapter(vaultAdapter);
    }
}
