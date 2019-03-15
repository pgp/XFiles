package it.pgp.xfiles.smbclient;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.sftpclient.InsertEditDialog;
import it.pgp.xfiles.sftpclient.VaultAdapter;

public class SmbVaultAdapter extends VaultAdapter {

    public SmbVaultAdapter(Context context, Activity mainActivity, Map<Long, ?> loginItemsDbMap) {
        super(context, mainActivity, loginItemsDbMap);
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.smb_list_item, null);
        }
        final SmbAuthData loginItem = (SmbAuthData) loginItems.get(position);

        //Handle TextView and display string from your list
        TextView user = view.findViewById(R.id.smb_listitem_user);
        TextView domain = view.findViewById(R.id.smb_listitem_domain);
        TextView host = view.findViewById(R.id.smb_listitem_host);
        TextView port = view.findViewById(R.id.smb_listitem_port);
        // Not needed to see passwords within adapter

        user.setText(loginItem.username);
        domain.setText(loginItem.domain);
        host.setText(loginItem.host);
        port.setText(loginItem.port+"");


        //Handle buttons and add onClickListeners
        ImageButton editBtn = view.findViewById(R.id.passitem_edit);
        ImageButton deleteBtn = view.findViewById(R.id.passitem_delete);

        editBtn.setOnClickListener(v -> {
            InsertEditDialog insertEditDialog = new InsertEditDialog(mainActivity,SmbVaultAdapter.this,dbIds.get(position), (SmbAuthData) loginItems.get(position));
            insertEditDialog.show();
        });
        deleteBtn.setOnClickListener(v -> {
            // remove row from db
//            boolean deleted = dbh.deleteRowFromSftpTable(dbIds.get(position));
            boolean deleted = dbh.deleteRowFromTable(SmbAuthData.ref,dbIds.get(position));
            if(deleted) {
                // remove row from loginItems
                dbIds.remove(position);
                loginItems.remove(position);
            }
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(context,message, Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return view;
    }
}
