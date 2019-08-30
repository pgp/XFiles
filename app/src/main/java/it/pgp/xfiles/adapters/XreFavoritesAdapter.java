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
import it.pgp.xfiles.dialogs.InsertEditXreFavoritesDialog;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 22/09/17
 */

public class XreFavoritesAdapter extends BaseAdapter implements ListAdapter {
    private final Context context;
    private ArrayList<String> xreFavoriteServers;
    private ArrayList<String> xreFavoritePaths;
    private ArrayList<Long> dbIds;
    private GenericDBHelper dbh;

    XreFavoritesAdapter(final Context context) {
        this.context = context;
        this.dbh = new GenericDBHelper(context);
        this.dbIds = new ArrayList<>();
        this.xreFavoriteServers = new ArrayList<>();
        this.xreFavoritePaths = new ArrayList<>();

        Map<Long,Map.Entry<String,String>> lfDbMap;
        lfDbMap = dbh.getAllRowsOfXreFavoritesTable();

        for (Map.Entry<Long,Map.Entry<String,String>> entry : lfDbMap.entrySet()) {
            dbIds.add(entry.getKey());
            xreFavoriteServers.add(entry.getValue().getKey());
            xreFavoritePaths.add(entry.getValue().getValue());
        }
    }

    public void syncInsertFromDialog(Long oid, String server, String path) {
        dbIds.add(oid);
        xreFavoriteServers.add(server);
        xreFavoritePaths.add(path);
        notifyDataSetChanged();
    }

    public void syncEditFromDialog(Long oldOid,
                                   Long newOid,
                                   String oldServer,
                                   String newServer,
                                   String oldPath,
                                   String newPath) {
        // this code is safe because xreFavorites cannot contain duplicates due to the primary key
        // constraint on the db table
        dbIds.remove(oldOid);
        xreFavoriteServers.remove(oldServer);
        xreFavoritePaths.remove(oldPath);
        dbIds.add(newOid);
        xreFavoriteServers.add(newServer);
        xreFavoritePaths.add(newPath);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dbIds.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
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
            view = inflater.inflate(R.layout.favorites_xre_list_item, null);
        }
        final String xreServer = xreFavoriteServers.get(position);
        final String xrePath = xreFavoritePaths.get(position);

        //Handle TextView and display string from your list
        TextView server = view.findViewById(R.id.favorites_xre_list_item_server);
        TextView path = view.findViewById(R.id.favorites_xre_list_item_path);
        server.setText(xreServer);
        path.setText(xrePath);

        //Handle buttons and add onClickListeners
        ImageButton editBtn = view.findViewById(R.id.favorites_xre_list_item_edit);
        ImageButton deleteBtn = view.findViewById(R.id.favorites_xre_list_item_delete);

        editBtn.setOnClickListener(v -> new InsertEditXreFavoritesDialog(
                context,
                XreFavoritesAdapter.this,
                getItemId(position),
                xreFavoriteServers.get(position),
                xreFavoritePaths.get(position)).show());
        deleteBtn.setOnClickListener(v -> {
            // remove row from db
            boolean deleted = dbh.deleteRowFromXreFavoritesTable(dbIds.get(position));
            if(deleted) {
                // remove row
                dbIds.remove(position);
                xreFavoriteServers.remove(position);
                xreFavoritePaths.remove(position);
            }
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return view;
    }
}
