package it.pgp.xfiles.dialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.HashAlgorithmsAdapter;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;

/**
 * Created by pgp on 21/02/18
 */

public class ChecksumActivity extends EffectActivity {

    private ChecksumTask checksumTask;
//    private BasePathContent file;

    private List<BasePathContent> files;

//    private Map<HashRequestCodes,CheckBox> selector;
    private HashAlgorithmsAdapter adapter;
    private TableLayout standardResultsLayout;
    private Button computeChecksumsButton;

    private ClipboardManager clipboard;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // LEGACY, file path passed as string, valid only for local files
        /*String singleFile = getIntent().getStringExtra("file");
        if (singleFile == null) {
            files = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItemsAsPathContents();
        }
        else {
            files = Collections.singletonList(new LocalPathContent(singleFile));
        }*/

        // NEW, file path passed as BasePathContent, also for XRE paths
        BasePathContent singleFile = (BasePathContent) getIntent().getSerializableExtra("pathcontent");
        if (singleFile == null)
            files = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItemsAsPathContents();
        else
            files = Collections.singletonList(singleFile);

        setContentView(R.layout.checksum_base_dialog);
        setTitle("Checksum");

        standardResultsLayout = findViewById(R.id.standardResultsLayout);

        GridView hashSelectorView = findViewById(R.id.hashSelectorView);
        hashSelectorView.setOnItemClickListener((parent, item, position, id) -> {
            HashRequestCodes h = adapter.getItem(position);
            h.toggleChecked();
            HashAlgorithmsAdapter.ViewHolder viewHolder = (HashAlgorithmsAdapter.ViewHolder) item.getTag();
            viewHolder.cb.setChecked(h.isChecked());
        });

        adapter = new HashAlgorithmsAdapter(this);
        hashSelectorView.setAdapter(adapter);

        computeChecksumsButton = findViewById(R.id.computeChecksumsButton);
        computeChecksumsButton.setOnClickListener(v -> {
            checksumTask = new ChecksumTask();
            checksumTask.execute((Void[])null);
        });

    }

    HashTextView currentlySelectedTableItem;

    // context menu for long click on hashtextview in the table layout
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (!(v instanceof HashTextView)) {
            Toast.makeText(this, "Context selection is not a HashTextView", Toast.LENGTH_SHORT).show();
            return;
        }
        currentlySelectedTableItem = (HashTextView) v;
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_checksum, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.checksumCopyHashToClipboard:
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        currentlySelectedTableItem.getHeader(),
                        currentlySelectedTableItem.getContent()));
                break;
            case R.id.checksumCopyFullInfoToClipboard:
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        currentlySelectedTableItem.getHeader(),
                        currentlySelectedTableItem.getText()));
                break;
            default:
                return true;
        }
        Toast.makeText(this, "Hash copied to clipboard", Toast.LENGTH_SHORT).show();
        return true;
    }

    private class HashTextView extends android.support.v7.widget.AppCompatTextView {

        private CharSequence header = "";
        private CharSequence content = "";

        public HashTextView(Context context,
                            CharSequence content,
                            CharSequence header) {
            super(context);
            this.content = content;
            this.header = header;
            setText(header+": "+content);
        }

        public CharSequence getHeader() {
            return header;
        }

        public CharSequence getContent() {
            return content;
        }
    }

    private boolean checksumInterrupted = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        checksumInterrupted = true;
        HashRequestCodes.clear();
    }

    private class ChecksumTask extends AsyncTask<Void, Void, Void> {
        final Set<HashRequestCodes> selectedHashAlgorithms = adapter.getSelectedItems();
        final boolean someSelection = selectedHashAlgorithms.size()>0;

        @Override
        protected void onPreExecute() {
            computeChecksumsButton.setEnabled(false);
            standardResultsLayout.removeAllViews();
        }

        private byte[] computeHashForLocalOrXREPaths(BasePathContent path, HashRequestCodes s) throws IOException {
            switch (path.providerType) {
                case LOCAL:
                case XFILES_REMOTE:
                    return MainActivity.currentHelper.hashFile(path,s);
                default:
                    throw new RuntimeException("Only local and XRE paths allowed for hashing");
            }
        }

        @Override
        protected Void doInBackground(Void... unused) {
            final int[][] tvBackground = new int[][]{{Color.DKGRAY,Color.BLUE},{Color.RED,Color.GRAY}};

            // TODO restructure hash request in RH protocol, allow multiple hashes per multiple files
            // then, make the RH task cancellable via another sub-request type (like in FindUpdatesThread)
            int i = 0,j=0;
            try {
                if(!someSelection) return null;
                if (files.size()==1) { // algorithms on rows, 1 column (only 1 file)
                    BasePathContent file = files.get(0);
                    for (HashRequestCodes s : selectedHashAlgorithms) {
                        if (checksumInterrupted) {
                            MainActivity.showToastOnUI("Checksum task interrupted");
                            return null;
                        }
                        TableRow tr = new TableRow(ChecksumActivity.this);
                        runOnUiThread(()->standardResultsLayout.addView(tr));

                        byte[] digest = computeHashForLocalOrXREPaths(file,s);

                        // run on UI thread
                        HashTextView t = new HashTextView(
                                ChecksumActivity.this,
                                Misc.toHexString(digest),
                                file.getName()+", "+s.getLabel() );

                        t.setBackgroundColor(tvBackground[i][j++%2]);
                        registerForContextMenu(t);
                        runOnUiThread(()->tr.addView(t));
                    }
                }
                else for (BasePathContent file : files) { // files on rows
                    TableRow tr = new TableRow(ChecksumActivity.this);
                    runOnUiThread(()->standardResultsLayout.addView(tr));

                    for (HashRequestCodes s : selectedHashAlgorithms) {
                        if (checksumInterrupted) {
                            MainActivity.showToastOnUI("Checksum task interrupted");
                            return null;
                        }

                        byte[] digest = computeHashForLocalOrXREPaths(file,s);

                        // run on UI thread
                        HashTextView t = new HashTextView(
                                ChecksumActivity.this,
                                Misc.toHexString(digest),
                                file.getName()+", "+s.getLabel() );

                        t.setBackgroundColor(tvBackground[i][j++%2]);
                        registerForContextMenu(t);
                        runOnUiThread(()->tr.addView(t));

                    }
                    i=(i+1)%2;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                MainActivity.showToastOnUI("Error during checksum computation");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(checksumInterrupted) return;
            if(!someSelection)
                Toast.makeText(ChecksumActivity.this,"No checksum algorithm selected",Toast.LENGTH_SHORT).show();
            computeChecksumsButton.setEnabled(true);
        }
    }
}
