package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.SftpFavoritesAdapter;
import it.pgp.xfiles.utils.FavoritesList;
import it.pgp.xfiles.utils.GenericDBHelper;

/**
 * Created by pgp on 05/07/17
 * Insert and edit mode are equal here, they both translate into an update to the serialized list blob
 * of an existing sftp/smb credential record
 */

public class InsertEditSftpFavoritesDialog extends Dialog {

    // Edit mode if currentFavoritePath non null, insert mode otherwise
    // currentFavoritePath is a member of currentFavorites list
    // currentFavorites list is updated before serialization, by replacing currentFavoritePath with
    // the content of the EditText
    // currentFavoritePath is unique
    // currentFavorites is a reference contained in the sfdbMap, so its modification
    // need not be explicitly passed back, it is sufficent to refill the positional arrays in syncEditFromDialog
    public InsertEditSftpFavoritesDialog(
            final Object ref,
            final Context context,
            final SftpFavoritesAdapter adapter,
            final long currentOid,
            final FavoritesList currentFavorites,
            final @Nullable String currentFavoritePath) {
        super(context);
        setContentView(R.layout.single_filename_dialog);
        EditText favorite = findViewById(R.id.singleFilenameEditText);
        if (currentFavoritePath != null)
            favorite.setText(currentFavoritePath);

        Button ok = findViewById(R.id.singleFilenameOkButton);
        ok.setOnClickListener(v -> {
            GenericDBHelper dbh = new GenericDBHelper(context);
            // update favorites list: remove old path if not null, insert new
            if (currentFavoritePath != null)
                currentFavorites.paths.remove(currentFavoritePath);
            currentFavorites.paths.add(favorite.getText().toString());

            // update only, leave oid unchanged
//            if (dbh.updateSftpFavs(currentOid,currentFavorites.paths)) {
            if (dbh.updateFavs(ref,currentOid,currentFavorites.paths)) {
                // update visualization
                adapter.syncEditFromDialog();
                Toast.makeText(context,"Edit successful",Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context,"Edit failed",Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

    }
}
