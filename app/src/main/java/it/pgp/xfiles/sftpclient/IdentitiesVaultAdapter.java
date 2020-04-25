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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 12/02/17
 */

public class IdentitiesVaultAdapter extends BaseAdapter implements ListAdapter {
    private final VaultActivity vaultActivity;
    private final List<String> idsFilenames = new ArrayList<>();
    private final List<String> idsHashes = new ArrayList<>();
    private final List<String> idsTypes = new ArrayList<>();
    private final File idsDir;
    private final SSHClient clientForKeyParsing = new SSHClient();

    // TODO on choosing private key, if public one is present, copy it as well
    public static final FilenameFilter idFilter = (dir, name) -> {
        return !name.endsWith(".pub") && !name.equals("known_hosts"); // follow .ssh standard folder content (do not place known_hosts in another directory)
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

        showBtn.setOnClickListener(v -> {
            /* TODO show basic text dialog with file content*/
            MainActivity.mainActivity.goDir(
                    new LocalPathContent(idsDir.getAbsolutePath()),
                    MainActivity.mainActivity.browserPager.getCurrentItem(),
                    idFilename
                    );
            vaultActivity.finish();
        });
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
        idsTypes.clear();
        idsHashes.clear();
        File[] files = idsDir.listFiles(idFilter);
        if (files != null) {
            for (File x : files) {
                idsFilenames.add(x.getName());
                Pair<KeyType, String> p = getKeyTypeAndPubkeyFingerprint(x);
                idsTypes.add(p.i.name());
                idsHashes.add(p.j);
            }
        }

        super.notifyDataSetChanged();
    }

    private Pair<KeyType,String> getKeyTypeAndPubkeyFingerprint(File f) {
        try {
            KeyProvider kprov = clientForKeyParsing.loadKeys(f.getAbsolutePath());
            String fingerprint = SecurityUtils.getFingerprint(kprov.getPublic());
            return new Pair<>(kprov.getType(), fingerprint);
        }
        catch(Exception e) {
            e.printStackTrace();
            return new Pair<>(KeyType.UNKNOWN,"");
        }
    }

    /*// sloppy parsing, just checks for BEGIN line
    private static SshKeyType getKeyTypeFromHeader(File f) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            if(line != null) {
                if(line.startsWith("-----BEGIN RSA")) return SshKeyType.RSA;
                else if(line.startsWith("-----BEGIN OPENSSH")) return SshKeyType.ED25519; // FIXME not only ed25519 keys can be encoded in cusotm openssh format
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }*/
}
