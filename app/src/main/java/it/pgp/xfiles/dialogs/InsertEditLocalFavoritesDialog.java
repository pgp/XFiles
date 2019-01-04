package it.pgp.xfiles.dialogs;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.LocalFavoritesAdapter;
import it.pgp.xfiles.sftpclient.InsertFailedException;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class InsertEditLocalFavoritesDialog extends ImmersiveModeDialog {
    private EditText fullPath;
    private Button ok;
    private GenericDBHelper dbh;

    // insert mode
    public InsertEditLocalFavoritesDialog(final Context context,
                                          final LocalFavoritesAdapter adapter) {
        super(context);
        setContentView(R.layout.single_filename_dialog);
        setTitle("Add local favorite");
        dbh = new GenericDBHelper(context);
        fullPath = findViewById(R.id.singleFilenameEditText);
        ok = findViewById(R.id.singleFilenameOkButton);

        ok.setOnClickListener(v -> {
            try {
                // TODO validate path content
                Map.Entry<Long,String> entry = dbh.addLocalFavorite(fullPath.getText().toString());
                Toast.makeText(context, "Favorite added", Toast.LENGTH_SHORT).show();
                adapter.syncInsertFromDialog(entry.getKey(),entry.getValue());
                dismiss();
            }
            catch (InsertFailedException e) {
                Toast.makeText(context, "Favorite already exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // edit mode
    public InsertEditLocalFavoritesDialog(final Context context,
                                          final LocalFavoritesAdapter adapter,
                                          final long currentOid,
                                          final String currentPath) {
        super(context);
        setContentView(R.layout.single_filename_dialog);
        setTitle("Add local favorite");
        dbh = new GenericDBHelper(context);
        fullPath = findViewById(R.id.singleFilenameEditText);
        fullPath.setText(currentPath);
        ok = findViewById(R.id.singleFilenameOkButton);

        ok.setOnClickListener(v -> {
            // TODO validate path content
            String newPath = fullPath.getText().toString();
            if (newPath.equals(currentPath)) {
                Toast.makeText(context, "Old path not modified", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbh.updateLocalFavorite(currentPath,newPath)) {
                Toast.makeText(context, "Favorite updated", Toast.LENGTH_SHORT).show();
                // notify dataset changed in adapter (same oid)
                adapter.syncEditFromDialog(currentOid,currentOid,currentPath,newPath);
                dismiss();
            }
            else {
                Toast.makeText(context, "Favorite update error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
