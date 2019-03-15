package it.pgp.xfiles.sftpclient;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 11/02/17 (adapted from KeyGuard)
 */

public class VaultAdapter extends BaseAdapter implements ListAdapter {
    public final Context context;
    public final Activity mainActivity;
    public ArrayList loginItems;
    public ArrayList<Long> dbIds;
    public GenericDBHelper dbh;


    // TODO generalize or extend class
    public VaultAdapter(final Context context, final Activity mainActivity, Map<Long,?> loginItemsDbMap) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.dbh = new GenericDBHelper(context);
        this.loginItems = new ArrayList(loginItemsDbMap.values());
        this.dbIds = new ArrayList<>(loginItemsDbMap.keySet());
    }

    public void syncInsertFromDialog(Long oid, Object u) {
        dbIds.add(oid);
        loginItems.add(u);
        notifyDataSetChanged();
    }

    public void syncEditFromDialog(Long oldOid, Long newOid, Object oldU, Object newU) {
        dbIds.add(newOid);
        loginItems.add(newU);
        dbIds.remove(oldOid);
        loginItems.remove(oldU);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return loginItems.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.sftp_list_item, null);
        }
        final AuthData loginItem = (AuthData) loginItems.get(position);

        //Handle TextView and display string from your list
        TextView user = view.findViewById(R.id.sftp_listitem_user);
        TextView domain = view.findViewById(R.id.sftp_listitem_domain);
        TextView port = view.findViewById(R.id.sftp_listitem_port);
        // Not needed to see passwords within adapter

        user.setText(loginItem.username);
        domain.setText(loginItem.domain);
        port.setText(loginItem.port+"");


        //Handle buttons and add onClickListeners
        ImageButton editBtn = view.findViewById(R.id.passitem_edit);
        ImageButton deleteBtn = view.findViewById(R.id.passitem_delete);

        editBtn.setOnClickListener(v -> {
            InsertEditDialog insertEditDialog = new InsertEditDialog(mainActivity,VaultAdapter.this,dbIds.get(position), (AuthData) loginItems.get(position));
            insertEditDialog.show();
        });
        deleteBtn.setOnClickListener(v -> {
            // remove row from db
//            boolean deleted = dbh.deleteRowFromSftpTable(dbIds.get(position));
            boolean deleted = dbh.deleteRowFromTable(AuthData.ref,dbIds.get(position));
            if(deleted) {
                // remove row from loginItems
                dbIds.remove(position);
                loginItems.remove(position);
            }
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return view;
    }
}
