package it.pgp.xfiles.adapters;

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
import it.pgp.xfiles.dialogs.InsertEditLocalFavoritesDialog;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 04/07/17
 */

public class LocalFavoritesAdapter extends BaseAdapter implements ListAdapter {
    private final Context context;
    private ArrayList<String> localFavorites;
    private ArrayList<Long> dbIds;
    private GenericDBHelper dbh;

    LocalFavoritesAdapter(final Context context) {
        this.context = context;
        this.dbh = new GenericDBHelper(context);
        this.dbIds = new ArrayList<>();
        this.localFavorites = new ArrayList<>();

        Map<Long,String> lfDbMap;
        lfDbMap = dbh.getAllRowsOfLocalFavoritesTable();

        for (Map.Entry<Long,String> entry : lfDbMap.entrySet()) {
            dbIds.add(entry.getKey());
            localFavorites.add(entry.getValue());
        }
    }

    public void syncInsertFromDialog(Long oid, String u) {
        dbIds.add(oid);
        localFavorites.add(u);
        notifyDataSetChanged();
    }

    public void syncEditFromDialog(Long oldOid, Long newOid, String oldU, String newU) {
        // this code is safe because localFavorites cannot contain duplicates due to the primary key
        // constraint on the db table
        dbIds.remove(oldOid);
        localFavorites.remove(oldU);
        dbIds.add(newOid);
        localFavorites.add(newU);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dbIds.size();
    }

    @Override
    public String getItem(int position) {
        return localFavorites.get(position);
    }

    @Override
    public long getItemId(int position) {
        return dbIds.get(position);
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.favorites_local_list_item, null);
        }
        final String localPath = localFavorites.get(position);

        //Handle TextView and display string from your list
        TextView path = view.findViewById(R.id.favorites_local_list_item_path);
        path.setText(localPath);

        //Handle buttons and add onClickListeners
        ImageButton editBtn = view.findViewById(R.id.favorites_local_list_item_edit);
        ImageButton deleteBtn = view.findViewById(R.id.favorites_local_list_item_delete);

        editBtn.setOnClickListener(v -> {
            new InsertEditLocalFavoritesDialog(
                    context,
                    LocalFavoritesAdapter.this,
                    getItemId(position),
                    getItem(position)).show();
        });
        deleteBtn.setOnClickListener(v -> {
            // remove row from db
            boolean deleted = dbh.deleteRowFromLocalFavoritesTable(dbIds.get(position));
            if(deleted) {
                // remove row from localFavorites
                dbIds.remove(position);
                localFavorites.remove(position);
            }
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return view;
    }
}
