package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 11/02/17 (adapted from KeyGuard)
 */

public class InsertEditDialog extends Dialog {

    EditText user,domain,port,password;
    Button okButton;
    GenericDBHelper dbh;

    final VaultAdapter vaultAdapter;

    public void setCommonDialogLayout(final Context context) {
        dbh = new GenericDBHelper(context);
        this.setContentView(R.layout.sftp_dialog_insert_item);
        this.setTitle("SFTP credentials");


        user= findViewById(R.id.insertUsernameEditText);
        domain= findViewById(R.id.insertDomainEditText);
        port= findViewById(R.id.insertPortEditText);
        password = findViewById(R.id.insertPasswordEditText);

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
                Map.Entry<Long,AuthData> entry =
                        dbh.insertSftpCred(user.getText().toString(),
                                domain.getText().toString(),
                                Integer.valueOf(port.getText().toString()),
                                password.getText().toString());
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
    public InsertEditDialog(final Context context, final VaultAdapter vaultAdapter, final long currentOid, final AuthData currentItem) {
        super(context);
        this.vaultAdapter = vaultAdapter;
        setCommonDialogLayout(context); // common layout initialization

        // populate fields with current item content
        user.setText(currentItem.username);
        domain.setText(currentItem.domain);
        port.setText(currentItem.port+"");
        password.setText(currentItem.password);

        okButton.setOnClickListener(v -> {
            // old way (insert new, then delete old)
//                try {
//                    // remove old
//                    dbh.deleteRow(currentOid);
//                    // insert new
//                    Map.Entry<Long,AuthData> entry =
//                            dbh.insertRow(user.getText().toString(),
//                                    domain.getText().toString(),
//                                    Integer.valueOf(port.getText().toString()),
//                                    password.getText().toString()
//                                    );
//                    // update visualization
//                    vaultAdapter.syncEditFromDialog(currentOid,entry.getKey(),currentItem,entry.getValue());
//                    Toast.makeText(vaultActivity,"Edit successful",Toast.LENGTH_SHORT).show();
//                } catch (InsertFailedException e) {
//                    Toast.makeText(vaultActivity,"Edit failed",Toast.LENGTH_SHORT).show();
//                }

            // update only, leave oid unchanged
            AuthData newAuthData = new AuthData(
                    user.getText().toString(),
                    domain.getText().toString(),
                    Integer.valueOf(port.getText().toString()),
                    password.getText().toString());
            if (dbh.updateSftpCred(currentOid,newAuthData)) {
                // update visualization
                vaultAdapter.syncEditFromDialog(currentOid,currentOid,currentItem,newAuthData);
                Toast.makeText(context,"Edit successful",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context,"Edit failed",Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });
    }
}

