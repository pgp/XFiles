package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.smbclient.SmbAuthData;
import it.pgp.xfiles.smbclient.SmbVaultAdapter;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 11/02/17 (adapted from KeyGuard)
 * modified on 15/03/19 (common for both SFTP and SMB)
 */

public class InsertEditDialog extends Dialog {

    EditText user,domain,host,port,password;
    TextView hostLabel;
    Button okButton;
    GenericDBHelper dbh;

    final VaultAdapter vaultAdapter;

    public void setCommonDialogLayout(final Context context) {
        dbh = new GenericDBHelper(context);
        this.setContentView(R.layout.sftp_dialog_insert_item);
        this.setTitle("Remote credentials");

        user= findViewById(R.id.insertUsernameEditText);
        domain= findViewById(R.id.insertDomainEditText);
        host= findViewById(R.id.insertHostEditText);
        hostLabel= findViewById(R.id.insertHostLabel);
        port= findViewById(R.id.insertPortEditText);
        password = findViewById(R.id.insertPasswordEditText);

        if(!(vaultAdapter instanceof SmbVaultAdapter)) {
            hostLabel.setVisibility(View.GONE);
            host.setVisibility(View.GONE);
        }

        okButton = findViewById(R.id.insertItemOkButton);
    }

    // insert mode
    public InsertEditDialog(final Context context, final VaultAdapter vaultAdapter) {
        super(context);
        this.vaultAdapter = vaultAdapter;
        setCommonDialogLayout(context); // common layout initialization
        okButton.setOnClickListener(v -> {
            // insert row using DBHelper, then propagate the returned inserted entry to MainActivity
            try {
                Map.Entry<Long,?> entry =
                        dbh.insertCred(
                                vaultAdapter instanceof SmbVaultAdapter?SmbAuthData.ref:AuthData.ref,
                                user.getText().toString(),
                                domain.getText().toString(),
                                Integer.valueOf(port.getText().toString()),
                                password.getText().toString(),
                                vaultAdapter instanceof SmbVaultAdapter?new String[]{host.getText().toString()}:new String[]{});
                vaultAdapter.syncInsertFromDialog(entry.getKey(),entry.getValue());
                Toast.makeText(context,"Insert successful",Toast.LENGTH_SHORT).show();
            }
            catch (InsertFailedException e) {
                Toast.makeText(context,"Insert failed",Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
    }

    // edit mode
    public InsertEditDialog(final Context context, final VaultAdapter vaultAdapter, final long currentOid, final Object currentItem_) {
        super(context);
        this.vaultAdapter = vaultAdapter;
        setCommonDialogLayout(context); // common layout initialization

        // populate fields with current item content
        if(vaultAdapter instanceof SmbVaultAdapter) {
            SmbAuthData currentItem = (SmbAuthData)currentItem_;
            user.setText(currentItem.username);
            domain.setText(currentItem.domain);
            host.setText(currentItem.host);
            port.setText(currentItem.port+"");
            password.setText(currentItem.password);
        }
        else {
            AuthData currentItem = (AuthData)currentItem_;
            user.setText(currentItem.username);
            domain.setText(currentItem.domain);
            port.setText(currentItem.port+"");
            password.setText(currentItem.password);
        }

        okButton.setOnClickListener(v -> {

            // update only, leave oid unchanged

            Object newAuthData;
            if (vaultAdapter instanceof SmbVaultAdapter)
                newAuthData = new SmbAuthData(
                        user.getText().toString(),
                        domain.getText().toString(),
                        host.getText().toString(),
                        Integer.valueOf(port.getText().toString()),
                        password.getText().toString());
            else
                newAuthData = new AuthData(
                        user.getText().toString(),
                        domain.getText().toString(),
                        Integer.valueOf(port.getText().toString()),
                        password.getText().toString());

            if (dbh.updateCred(currentOid,newAuthData)) {
                // update visualization
                vaultAdapter.syncEditFromDialog(currentOid,currentOid,currentItem_,newAuthData);
                Toast.makeText(context,"Edit successful",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context,"Edit failed",Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });
    }
}

