package it.pgp.xfiles.dialogs;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.XreFavoritesAdapter;
import it.pgp.xfiles.sftpclient.InsertFailedException;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 22/09/17 (adapted from {@link InsertEditLocalFavoritesDialog})
 */
public class InsertEditXreFavoritesDialog extends ImmersiveModeDialog {
    private EditText fullPath;
    private EditText fullServer;
    private Button ok;
    private GenericDBHelper dbh;

    // insert mode
    public InsertEditXreFavoritesDialog(final Context context,
                                        final XreFavoritesAdapter adapter) {
        super(context);
        setContentView(R.layout.xre_path_dialog);
        setTitle("Add XFiles remote favorite");
        dbh = new GenericDBHelper(context);
        fullServer = findViewById(R.id.xre_server_edittext);
        fullPath = findViewById(R.id.xre_path_edittext);
        ok = findViewById(R.id.xre_ok_button);

        ok.setOnClickListener(v -> {
            try {
                // TODO validate path content
                Map.Entry<Long,Map.Entry<String,String>> entry = dbh.addXreFavorite(
                        fullServer.getText().toString(),
                        fullPath.getText().toString());
                Toast.makeText(context, "Favorite added", Toast.LENGTH_SHORT).show();
                adapter.syncInsertFromDialog(
                        entry.getKey(),
                        entry.getValue().getKey(),
                        entry.getValue().getValue()
                        );
                dismiss();
            }
            catch (InsertFailedException e) {
                Toast.makeText(context, "Favorite already exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // edit mode
    public InsertEditXreFavoritesDialog(final Context context,
                                        final XreFavoritesAdapter adapter,
                                        final long currentOid,
                                        final String currentServer,
                                        final String currentPath) {
        super(context);
        setContentView(R.layout.xre_path_dialog);
        setTitle("Add XFiles remote favorite");
        dbh = new GenericDBHelper(context);
        fullServer = findViewById(R.id.xre_server_edittext);
        fullPath = findViewById(R.id.xre_path_edittext);
        fullServer.setText(currentServer);
        fullPath.setText(currentPath);

        ok = findViewById(R.id.xre_ok_button);

        ok.setOnClickListener(v -> {
            // TODO validate path content
            String newServer = fullServer.getText().toString();
            String newPath = fullPath.getText().toString();
            if (newServer.equals(currentServer) && newPath.equals(currentPath)) {
                Toast.makeText(context, "Old data not modified", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbh.updateXreFavorite(
                    currentServer,
                    newServer,
                    currentPath,
                    newPath)) {
                Toast.makeText(context, "Favorite updated", Toast.LENGTH_SHORT).show();
                // notify dataset changed in adapter (same oid)
                adapter.syncEditFromDialog(
                        currentOid,
                        currentOid,
                        currentServer,
                        newServer,
                        currentPath,
                        newPath);
                dismiss();
            }
            else {
                Toast.makeText(context, "Favorite update error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
