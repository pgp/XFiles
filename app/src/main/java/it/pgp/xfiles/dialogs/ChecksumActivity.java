package it.pgp.xfiles.dialogs;

import android.app.Dialog;
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
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.HashAlgorithmsAdapter;
import it.pgp.xfiles.roothelperclient.HashRequestCodes;
import it.pgp.xfiles.utils.FileSaveFragment;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 21/02/18
 */

public class ChecksumActivity extends EffectActivity implements FileSaveFragment.Callbacks {

    private ChecksumTask checksumTask;

    private BasePathContent parentDir;
    private List<BrowserItem> files;

    private HashAlgorithmsAdapter adapter;
    private TableLayout standardResultsLayout;
    private Button computeChecksumsButton, exportChecksumsCSVButton, exportChecksumsJSONButton;

    private ClipboardManager clipboard;

    // directory hashing layout and checkboxes for options
    private LinearLayout dirHashOptsLayout;
    private CheckBox dirHashWithNames;
    private CheckBox dirHashIgnoreThumbsFiles;
    private CheckBox dirHashIgnoreUnixHiddenFiles;
    private CheckBox dirHashIgnoreEmptyDirs;

    private List<List<HashTextView>> hashMatrix = new ArrayList<>();
    private Set<HashRequestCodes> selectedHashAlgorithms; // selected from the last run (not necessarily completed)

    public void showLegend(View unused) {
        Dialog hashLegendDialog = new Dialog(this){
            @Override
            public void show() {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.hash_labels_legend);
                super.show();
            }
        };
        hashLegendDialog.show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // parent dir passed as BasePathContent, valid for both local and XRE paths
        BrowserItem singleFile = (BrowserItem) getIntent().getSerializableExtra("browseritem");
        if (singleFile == null)
            files = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItems();
        else
            files = Collections.singletonList(singleFile);

        parentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();

        setContentView(R.layout.checksum_base_dialog);
        setTitle("Checksum");

        standardResultsLayout = findViewById(R.id.standardResultsLayout);

        dirHashOptsLayout = findViewById(R.id.checksum_dirHashOptsLayout);

        // if there's at least a directory in the selection, enable dir hash options layout
        int dirHashOptsLayoutVisibility = View.GONE;
        for(BrowserItem b : files) {
            if(b.isDirectory) {
                dirHashOptsLayoutVisibility = View.VISIBLE;
                break;
            }
        }
        dirHashOptsLayout.setVisibility(dirHashOptsLayoutVisibility);

        dirHashWithNames = findViewById(R.id.checksum_dirHashWithNames);
        dirHashIgnoreThumbsFiles = findViewById(R.id.checksum_dirHashIgnoreThumbsFiles);
        dirHashIgnoreUnixHiddenFiles = findViewById(R.id.checksum_dirHashIgnoreUnixHiddenFiles);
        dirHashIgnoreEmptyDirs = findViewById(R.id.checksum_dirHashIgnoreEmptyDirs);

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
        exportChecksumsCSVButton = findViewById(R.id.exportChecksumsCSVButton);
        exportChecksumsJSONButton = findViewById(R.id.exportChecksumsJSONButton);

        // check SHA-256 by default
        Misc.getViewByPosition(HashRequestCodes.sha256.ordinal(),hashSelectorView).findViewById(R.id.checksum_checkbox).performClick();

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

    // FIXME duplicated from CompressActivity, refactor
    @Override
    public boolean onCanSave(String absolutePath, String fileName) {

        // Catch the really stupid case.
        if (absolutePath == null || absolutePath.length() ==0 ||
                fileName == null || fileName.length() == 0) {
            Toast.makeText(this,R.string.alert_supply_filename, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Do we have a filename if the extension is thrown away?
        String copyName = FileSaveFragment.NameNoExtension(fileName);
        if (copyName == null || copyName.length() == 0 ) {
            Toast.makeText(this,R.string.alert_supply_filename, Toast.LENGTH_SHORT).show();
            return false;
        }

        // No overwrite of an existing file.
        if (FileSaveFragment.FileExists(absolutePath, fileName)) {
            Toast.makeText(this,R.string.alert_file_exists, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onConfirmSave(String absolutePath, String fileName) {
        if (onCanSave(absolutePath,fileName)) {
            // export CSV only for now
            if(fileName.endsWith("csv"))
                exportToCSV(absolutePath,fileName);
            else exportToJSON(absolutePath,fileName);
        }
    }

    private static class HashTextView extends android.support.v7.widget.AppCompatTextView {

        private HashRequestCodes code;
        private CharSequence filename;
        private CharSequence content; // the digest as hex string

        public HashTextView(Context context,
                            CharSequence content,
                            CharSequence filename,
                            HashRequestCodes code) {
            super(context);
            this.content = content;
            this.code = code;
            this.filename = filename;
            setText(getHeader()+": "+content);
        }

        public CharSequence getHeader() {
            return filename+", "+code.getLabel();
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
        final boolean someSelection;

        {
            selectedHashAlgorithms = adapter.getSelectedItems();
            someSelection = selectedHashAlgorithms.size()>0;
        }

        @Override
        protected void onPreExecute() {
            computeChecksumsButton.setEnabled(false);
            standardResultsLayout.removeAllViews();
        }

        private byte[] computeHashForLocalOrXREPaths(BasePathContent path, HashRequestCodes s) throws IOException {
            switch (path.providerType) {
                case LOCAL:
                case XFILES_REMOTE:
                    BitSet dirHashOpts = new BitSet(4);
                    dirHashOpts.set(0,dirHashWithNames.isChecked());
                    dirHashOpts.set(1,dirHashIgnoreThumbsFiles.isChecked());
                    dirHashOpts.set(2,dirHashIgnoreUnixHiddenFiles.isChecked());
                    dirHashOpts.set(3,dirHashIgnoreEmptyDirs.isChecked());
                    return MainActivity.currentHelper.hashFile(path,s,dirHashOpts);
                default:
                    throw new RuntimeException("Only local and XRE paths allowed for hashing");
            }
        }

        @Override
        protected Void doInBackground(Void... unused) {
            hashMatrix = new ArrayList<>();
            final int[][] tvBackground = new int[][]{{Color.DKGRAY,Color.BLUE},{Color.RED,Color.GRAY}};

            // TODO restructure hash request in RH protocol, allow multiple hashes per multiple files
            // then, make the RH task cancellable via another sub-request type (like in FindUpdatesThread)
            int i=0, j=0;
            try {
                if(!someSelection) return null;
                if (files.size()==1) { // algorithms on rows, 1 column (only 1 file)
                    List<HashTextView> lhtv = new ArrayList<>(); // for csv/json export, keep the format coherent (1 row, multiple columns)
                    hashMatrix.add(lhtv);
                    BasePathContent file = parentDir.concat(files.get(0).getFilename());
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
                                file.getName(), s);

                        t.setBackgroundColor(tvBackground[i][j++%2]);
                        registerForContextMenu(t);
                        lhtv.add(t);
                        runOnUiThread(()->tr.addView(t));
                    }
                }
                else for (BrowserItem file : files) { // files on rows
                    TableRow tr = new TableRow(ChecksumActivity.this);
                    List<HashTextView> lhtv = new ArrayList<>();
                    hashMatrix.add(lhtv);
                    runOnUiThread(()->standardResultsLayout.addView(tr));

                    for (HashRequestCodes s : selectedHashAlgorithms) {
                        if (checksumInterrupted) {
                            MainActivity.showToastOnUI("Checksum task interrupted");
                            return null;
                        }

                        byte[] digest = computeHashForLocalOrXREPaths(parentDir.concat(file.getFilename()),s);

                        // run on UI thread
                        HashTextView t = new HashTextView(
                                ChecksumActivity.this,
                                Misc.toHexString(digest),
                                file.getFilename(), s);

                        t.setBackgroundColor(tvBackground[i][j++%2]);
                        registerForContextMenu(t);
                        lhtv.add(t);
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

    public void ok(View unused) {
        checksumTask = new ChecksumTask();
        checksumTask.execute((Void[])null);
    }

    public void openExportOutputSelector(View v) {
        if(hashMatrix.isEmpty()) {
            Toast.makeText(this, "No checksums to export", Toast.LENGTH_SHORT).show();
            return;
        }

        String fragTag = getResources().getString(R.string.tag_fragment_FileSave);

        String ext = v.getId()==R.id.exportChecksumsCSVButton?"csv":"json";

        // Get an instance supplying a default extension, captions and
        // icon appropriate to the calling application/activity.
        FileSaveFragment fsf = FileSaveFragment.newInstance(ext,
                R.string.alert_OK,
                R.string.alert_cancel,
                R.string.app_name,
                R.string.edit_hint,
                R.string.checksums_export_filename_header,
                R.drawable.xfiles_file_icon);
        fsf.show(getFragmentManager(), fragTag);
    }

    public void exportToCSV(String absolutePath, String fileName) {
        File csvFile = new File(absolutePath +"/"+fileName+".csv");
        try(OutputStream o = new BufferedOutputStream(new FileOutputStream(csvFile))) {
            // create header
            Misc.csvWriteRow(o,new ArrayList(){{
                add("filename");
                for (HashRequestCodes code : selectedHashAlgorithms) add(code.getLabel());
            }});

            for(List<HashTextView> lhtv : hashMatrix) {
                Misc.csvWriteRow(o,new ArrayList(){{
                    // by construction, lhtv contains hashes for the same filename
                    add(""+lhtv.get(0).filename);
                    for(HashTextView htv: lhtv) add(""+htv.content);
                }});
            }

            MainActivity.showToastOnUI("Checksums export complete");
        } catch (IOException e) {
            e.printStackTrace();
            MainActivity.showToastOnUI("Error exporting checksums to CSV");
        }
    }

    public void exportToJSON(String absolutePath, String fileName) {
        List l = new ArrayList();
        for(List<HashTextView> lhtv : hashMatrix) {
            Map m = new HashMap();
            Map n = new HashMap();
            for(HashTextView htv : lhtv) {
                n.put(htv.code.getLabel(),""+htv.content);
            }
            m.put("filename",""+lhtv.get(0).filename);
            m.put("checksums",n);
            l.add(m);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(absolutePath+"/"+fileName+".json"),l);
            MainActivity.showToastOnUI("Checksums export complete");
        }
        catch (IOException e) {
            e.printStackTrace();
            MainActivity.showToastOnUI("Error exporting checksums to JSON");
        }
    }
}
