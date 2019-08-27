package it.pgp.xfiles.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.InsertEditSftpFavoritesDialog;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.smbclient.SmbAuthData;
import it.pgp.xfiles.utils.FavoritesList;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 04/07/17
 *
 * web sources for multiple-kind-of-row listview:
 *  - http://stackoverflow.com/questions/4777272/android-listview-with-different-layouts-for-each-row
 *  - http://android.amberfog.com/?p=296
 */

public class SftpFavoritesAdapter extends BaseAdapter {
    public static final int HEADER_TYPE = 0;
    public static final int CONTENT_TYPE = 1;

    final Context context;

    ArrayList<Object> adapterItems; // objects can be (Smb)AuthData (header) or String (content)
    ArrayList<Long> adapterDbIds;

    TreeMap<Long, FavoritesList> sfDbMap; // for preserving explicit position ordering

    GenericDBHelper dbh;

    protected void refillArrays() {
        adapterDbIds = new ArrayList<>();
        adapterItems = new ArrayList<>();
        notifyDataSetChanged(); // this additional notify is needed for making edit mode working (on edit, number of items remains the same, and since positions are not changed as well, observers of base adapter wouldn't notice the difference, leaving old values in the views)
        for (Map.Entry<Long,FavoritesList>  entry : sfDbMap.entrySet()) {
            adapterDbIds.add(entry.getKey());
            adapterItems.add(entry.getValue().a); // add header

            for (String path : ((FavoritesList<AuthData>)(entry.getValue())).paths) {
                adapterDbIds.add(entry.getKey());
                adapterItems.add(path); // add content
            }
        }
        notifyDataSetChanged();
    }

    public SftpFavoritesAdapter(final Context context) {
        this.context = context;
        this.dbh = new GenericDBHelper(context);

        init();
    }

    protected void init() {
        sfDbMap = new TreeMap<>(dbh.getAllCredsWithFavs(AuthData.ref));
        refillArrays();
    }

    // only edit here, every favorite insert/add/update is an update to the FavoritesList bean
    public void syncEditFromDialog() {
        refillArrays(); // inefficient, but avoids passing delta params for updating views
//        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return adapterItems.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2; // allowed types: header and content
    }

    @Override
    public int getItemViewType(int position) {
        Object o = adapterItems.get(position);

        if (o instanceof AuthData || o instanceof SmbAuthData) return HEADER_TYPE;
        else if (o instanceof String) return CONTENT_TYPE;
        else throw new RuntimeException("Unexpected adapter item type");
    }

    @Override
    public Object getItem(int position) {
        return adapterItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return adapterDbIds.get(position);
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        // list item can be of two types: header or content
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (getItemViewType(position) == HEADER_TYPE) {
                view = inflater.inflate(R.layout.sftp_list_header_item, null);
                TextView user = view.findViewById(R.id.sftp_listitem_user);
                TextView domain = view.findViewById(R.id.sftp_listitem_domain);
                TextView port = view.findViewById(R.id.sftp_listitem_port);
                ImageButton addButton = view.findViewById(R.id.sftp_add_favorite_button);
                user.setText(((AuthData)getItem(position)).username);
                domain.setText(((AuthData)getItem(position)).domain);
                port.setText(((AuthData)getItem(position)).port+"");
                addButton.setOnClickListener(v -> {
                    // insert mode
                    new InsertEditSftpFavoritesDialog(
                            AuthData.ref,
                            context,
                            SftpFavoritesAdapter.this,
                            getItemId(position),
                            sfDbMap.get(getItemId(position)),
                            null).show();
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
                    new InsertEditSftpFavoritesDialog(
                            AuthData.ref,
                            context,
                            SftpFavoritesAdapter.this,
                            getItemId(position),
                            sfDbMap.get(getItemId(position)),
                            (String) getItem(position)).show();
                });
                deleteBtn.setOnClickListener(v -> {
                    // remove row from db
                    FavoritesList<AuthData> currentFavorites = sfDbMap.get(getItemId(position));
                    currentFavorites.paths.remove(getItem(position));

                    // update only, leave oid unchanged
//                    if (dbh.updateSftpFavs(getItemId(position),currentFavorites.paths)) {
                    if (dbh.updateFavs(AuthData.ref,getItemId(position),currentFavorites.paths)) {
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
