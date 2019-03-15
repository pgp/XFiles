package it.pgp.xfiles.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.InsertEditSftpFavoritesDialog;
import it.pgp.xfiles.smbclient.SmbAuthData;
import it.pgp.xfiles.utils.FavoritesList;

public class SmbFavoritesAdapter extends SftpFavoritesAdapter {
    public SmbFavoritesAdapter(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        sfDbMap = new TreeMap<>(dbh.getAllCredsWithFavs(SmbAuthData.ref));
        refillArrays();
    }

    @Override
    protected void refillArrays() {
        adapterDbIds = new ArrayList<>();
        adapterItems = new ArrayList<>();
        notifyDataSetChanged(); // this additional notify is needed for making edit mode working (on edit, number of items remains the same, and since positions are not changed as well, observers of base adapter wouldn't notice the difference, leaving old values in the views)
        for (Map.Entry<Long,FavoritesList>  entry : sfDbMap.entrySet()) {
            adapterDbIds.add(entry.getKey());
            adapterItems.add(entry.getValue().a); // add header

            for (String path : ((FavoritesList<SmbAuthData>)(entry.getValue())).paths) {
                adapterDbIds.add(entry.getKey());
                adapterItems.add(path); // add content
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        // list item can be of two types: header or content
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (getItemViewType(position) == HEADER_TYPE) {
                view = inflater.inflate(R.layout.smb_list_header_item, null);
                TextView user = view.findViewById(R.id.smb_listitem_user);
                TextView domain = view.findViewById(R.id.smb_listitem_domain);
                TextView host = view.findViewById(R.id.smb_listitem_host);
                TextView port = view.findViewById(R.id.smb_listitem_port);
                ImageButton addButton = view.findViewById(R.id.smb_add_favorite_button);
                user.setText(((SmbAuthData)getItem(position)).username);
                domain.setText(((SmbAuthData)getItem(position)).domain);
                host.setText(((SmbAuthData)getItem(position)).host);
                port.setText(((SmbAuthData)getItem(position)).port+"");
                addButton.setOnClickListener(v -> {
                    // insert mode
                    InsertEditSftpFavoritesDialog insertEditDialog =
                            new InsertEditSftpFavoritesDialog(
                                    SmbAuthData.ref,
                                    context,
                                    SmbFavoritesAdapter.this,
                                    getItemId(position),
                                    sfDbMap.get(getItemId(position)),
                                    null);
                    insertEditDialog.show();
                });
            }
            else { // CONTENT_TYPE
                view = inflater.inflate(R.layout.favorites_local_list_item, null);
                TextView path = view.findViewById(R.id.favorites_local_list_item_path);

                path.setText((String)getItem(position));

                //Handle buttons and add onClickListeners
                ImageButton editBtn = view.findViewById(R.id.favorites_local_list_item_edit);
                ImageButton deleteBtn = view.findViewById(R.id.favorites_local_list_item_delete);

                editBtn.setOnClickListener(v -> {
                    // edit mode
                    InsertEditSftpFavoritesDialog insertEditDialog =
                            new InsertEditSftpFavoritesDialog(
                                    SmbAuthData.ref,
                                    context,
                                    SmbFavoritesAdapter.this,
                                    getItemId(position),
                                    sfDbMap.get(getItemId(position)),
                                    (String) getItem(position));
                    insertEditDialog.show();
                });
                deleteBtn.setOnClickListener(v -> {
                    // remove row from db
                    FavoritesList<SmbAuthData> currentFavorites = sfDbMap.get(getItemId(position));
                    currentFavorites.paths.remove(getItem(position));

                    // update only, leave oid unchanged
//                    if (dbh.updateSftpFavs(getItemId(position),currentFavorites.paths)) {
                    if (dbh.updateFavs(SmbAuthData.ref,getItemId(position),currentFavorites.paths)) {
                        // update visualization
                        // sfdbMap is updated from here, no need to pass params
                        syncEditFromDialog();
                        Toast.makeText(context,"Edit successful",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context,"Edit failed",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // no need to perform offensive coding here, getItemViewType already throws RuntimeException
        }

        return view;
    }
}
