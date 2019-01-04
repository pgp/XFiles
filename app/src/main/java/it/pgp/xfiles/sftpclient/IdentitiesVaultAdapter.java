package it.pgp.xfiles.sftpclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.R;

/**
 * Created by pgp on 12/02/17
 */

public class IdentitiesVaultAdapter extends BaseAdapter implements ListAdapter {
    private final VaultActivity vaultActivity;
    private final List<String> idsFilenames = new ArrayList<>();
    private final List<String> idsHashes = new ArrayList<>();
    private final List<String> idsTypes = new ArrayList<>();
    private final File idsDir;

    // TODO on choosing private key, if public one is present, copy it as well
    public static final FilenameFilter idFilter = (dir, name) -> {
        // second condition: no (.pub) extension (e.g. id_ecdsa)
        return name.startsWith("id_") &&
                name.indexOf('.') == -1; // follow .ssh standard folder content (do not place known_hosts in another directory ( TODO filename validation/rename needed on identity add )
    };

    IdentitiesVaultAdapter(final VaultActivity vaultActivity) {
        this.vaultActivity = vaultActivity;
        idsDir = new File(vaultActivity.getFilesDir(), SFTPProviderUsingPathContent.sshIdsDirName);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return idsFilenames.size();
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
            LayoutInflater inflater = (LayoutInflater) vaultActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.sftp_id_list_item, null);
        }
        String idFilename = idsFilenames.get(position);
        String idType = idsTypes.get(position);
        String idHash = idsHashes.get(position);

        //Handle TextView and display string from your list
        TextView filename = view.findViewById(R.id.sftp_id_listitem_filename);
        TextView type = view.findViewById(R.id.sftp_id_listitem_type);
        TextView hash = view.findViewById(R.id.sftp_id_listitem_hash);

        filename.setText(idFilename);
        type.setText(idType);
        hash.setText(idHash);

        //Handle buttons and add onClickListeners
        ImageButton showBtn = view.findViewById(R.id.sftp_id_listitem_show);
        ImageButton deleteBtn = view.findViewById(R.id.sftp_id_listitem_delete);

        showBtn.setOnClickListener(v -> {/* TODO show basic text dialog with file content*/});
        deleteBtn.setOnClickListener(v -> {
            String prvkname = idsFilenames.get(position);
            String pubkname = prvkname+".pub";
            File f = new File(idsDir,prvkname);
            File g = new File(idsDir,pubkname);
            g.delete(); // public key may not be present, don't indicate error
            boolean deleted = f.delete();
            String message=deleted?"Deleted!":"Delete error";
            Toast.makeText(vaultActivity,message,Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            vaultActivity.runOnUiThread(vaultActivity::showRefreshClientDialog);
        });

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        idsFilenames.clear();
        idsTypes.clear(); // TODO parse key types and add them to this list
        idsHashes.clear(); // TODO compute key hashes and add them to this list
        File[] files = idsDir.listFiles(idFilter);
        // DEBUG
        if (files != null) {
            for (File x : files) {
                idsFilenames.add(x.getName());
                idsTypes.add("DUMMY");
                idsHashes.add("00000000");
            }
        }

        super.notifyDataSetChanged();
    }
}
