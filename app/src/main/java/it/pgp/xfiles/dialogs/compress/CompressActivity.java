package it.pgp.xfiles.dialogs.compress;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.CopyListUris;
import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.OutputArchiveType;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.CompressService;
import it.pgp.xfiles.service.params.CompressParams;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.utils.FileSaveFragment;
import it.pgp.xfiles.utils.IntentUtil;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 20/11/17
 * Activity instead of Dialog in order to launch another dialog to browse for destination archive
 */

public class CompressActivity extends EffectActivity implements FileSaveFragment.Callbacks {

    boolean standaloneMode = false; // true if not started by MainActivity

    @Nullable BrowserItem filename;

    BasePathContent dirPath; // source dir path

    EditText outputArchiveFilePath;
    EditText outputArchivePassword;

    SeekBar compressionLevel;
    TextView compressionLevelNum;
    CheckedTextView encryptHeaders;
    CheckedTextView solidMode;

    ImageButton selectOutputArchiveFilePath;
    RadioGroup archiveTypeSelector;

    List<String> selectedItems;
    CopyListUris contentUris;


    boolean streamingArchivesEnabled = false;

    private void populateSelectedItems(Intent intent) {
        List<Uri> imageUris = IntentUtil.getShareSelectionFromIntent(intent);

        if (imageUris == null) {
            Log.e(getClass().getName(), "both extras are null, assuming CompressActivity was started by MainActivity...");
            standaloneMode = false;

            selectedItems = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItemsAsNameOnlyStrings();

            // compress one single item, only if no other item is selected
            if (selectedItems.size()==0 && filename != null)
                selectedItems = Collections.singletonList(filename.getFilename());

            // enable flag for streaming archive support only if single selection and the item is a file
            if(selectedItems.size() == 1) {
                List<BrowserItem> x = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItems();
                if((x.size()!=0 && !x.get(0).isDirectory)|| (x.size()==0 && !filename.isDirectory))
                    streamingArchivesEnabled = true;
            }

            ///////////////
            dirPath = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (dirPath.providerType != ProviderType.LOCAL) {
                Toast.makeText(this,"Create archive supported only on local filesystem",Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ///////////////

            return;
        }

        standaloneMode = true;

        try { // standard mode, deduce paths from uris
            Map.Entry<BasePathContent,List<String>> me = IntentUtil.getCommonAncestorAndItems(this,imageUris);
            dirPath = me.getKey();
            selectedItems = me.getValue();
            return;
        }
        catch(Exception e) { // content provider mode, extract paths from fds opened from uris,
            Log.e("COMPRESS","Path extraction from uri failed, trying with /proc/self/fd trick...",e);
        }

        try {
            Map.Entry<BasePathContent,List<String>> me = IntentUtil.getCommonAncestorAndItems_mode2(this,imageUris);
            dirPath = me.getKey();
            selectedItems = me.getValue();

            // try to access the resolved dirPath
            GenericDirWithContent dircontent = MainActivity.getRootHelperClient().listDirectory(dirPath);
            if (dircontent.errorCode != null) {
                throw new IOException("Common path extraction from /proc/self/fd succeeded, but resolved path is not accessible (e.g. ext sdcard original mount point, and no root access)");
            }
        }
        catch(IOException e) {
            Log.e("COMPRESS","Path extraction from /proc/self/fd failed",e);
            Toast.makeText(this,"Unable to extract paths from content uris, reverting to limited content provider mode",Toast.LENGTH_SHORT).show();
        }

        Log.e("COMPRESS", "entering limited content provider mode, only zip output format available");
        dirPath = new LocalPathContent(Misc.internalStorageDir.getAbsolutePath());
        selectedItems = null;
        contentUris = CopyListUris.getFromUriList(imageUris);

        // here, it's assumed 3rd party apps won't send URIs of directories via content provider
        if ((selectedItems!=null && selectedItems.size()==1) || (contentUris!=null && contentUris.contentUris.size()==1))
            streamingArchivesEnabled = true;
    }

    public void compress_ok(View unused) {
        BasePathContent filePath = new LocalPathContent(outputArchiveFilePath.getText().toString());
//        BasePathContent filePath = BasePathContent.concat(dirPath,outputArchiveFilePath.getText().toString());
        int idx = archiveTypeSelector.indexOfChild(
                archiveTypeSelector.findViewById(
                        archiveTypeSelector.getCheckedRadioButtonId()));
        String ext = OutputArchiveType.values()[idx].getValue();

        String destArchive = filePath.toString()+"."+ext;
        String srcFolder = dirPath.toString();

        // with service & task
        ////////////////////////
        Intent startIntent = new Intent(this,CompressService.class);
        startIntent.setAction(BaseBackgroundService.START_ACTION);
        startIntent.putExtra(
                "params",
                contentUris==null?
                        new CompressParams(
                                new LocalPathContent(srcFolder),
                                new LocalPathContent(destArchive),
                                compressionLevel.getProgress(),
                                encryptHeaders.isChecked(),
                                solidMode.isChecked(),
                                outputArchivePassword.getText().toString(),
                                selectedItems,
                                standaloneMode
                        ):
                        new CompressParams(
                                contentUris,
                                new LocalPathContent(destArchive),
                                compressionLevel.getProgress(),
                                encryptHeaders.isChecked(),
                                solidMode.isChecked(),
                                outputArchivePassword.getText().toString(),
                                standaloneMode
                        )
        );
        startService(startIntent);
//        finish(); // Security Manager prevents using content provider's file objects after the activity has ended
        if(contentUris==null) finish(); // back to MainActivity, or actually finishAffinity if in standalone mode, and not in limited content provider mode
        else MainActivity.simulateHomePress(this); // else, pause activity instead of finishing it
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // constructor params of CompressDialog are now passed in the bundle
        setTitle("Compress");
        setActivityIcon(R.drawable.xfiles_archive);
        setContentView(R.layout.compress_layout);
        MainActivity.refreshAppContext(getApplicationContext());
        MainActivity.getRootHelperClient();

        Intent intent = getIntent();

        filename = intent.getSerializableExtra("filename", BrowserItem.class); // single file to be compressed, if null use adapter selection

        populateSelectedItems(intent);

        outputArchiveFilePath = findViewById(R.id.outputArchiveFilePath);
        outputArchiveFilePath.setText(dirPath.concat((filename==null)?"archive":filename.getFilename()).toString());
        outputArchivePassword = findViewById(R.id.outputArchivePassword);

        compressionLevel = findViewById(R.id.compressionLevel);
        compressionLevelNum = findViewById(R.id.compressionLevelNum);
        int maxCompLevel = 9;
        compressionLevel.setMax(maxCompLevel); // max 7z compression level
        compressionLevel.setProgress(maxCompLevel);
        compressionLevelNum.setText(maxCompLevel+"");
        compressionLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                compressionLevelNum.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        encryptHeaders = findViewById(R.id.encryptFilenames);
        encryptHeaders.setOnClickListener(Misc.ctvListener);
        solidMode = findViewById(R.id.solidMode);
        solidMode.setOnClickListener(Misc.ctvListener);

        selectOutputArchiveFilePath = findViewById(R.id.selectOutputArchiveFilePath);
        selectOutputArchiveFilePath.setOnClickListener(this::openDestinationArchiveSelector);

        archiveTypeSelector = findViewById(R.id.archiveTypeRadioGroup);
        archiveTypeSelector.setOnCheckedChangeListener((radioGroup, i) -> {
            int idx = archiveTypeSelector.indexOfChild(
                    archiveTypeSelector.findViewById(
                            archiveTypeSelector.getCheckedRadioButtonId()));
            switch (OutputArchiveType.values()[idx]) {
                case _7Z:
                    // enable all
                    outputArchivePassword.setEnabled(true);
                    compressionLevel.setEnabled(true);
                    encryptHeaders.setEnabled(true);
                    solidMode.setEnabled(true);
                    break;
                case ZIP:
                    // no solid mode nor encrypt headers supported
                    outputArchivePassword.setEnabled(true);
                    compressionLevel.setEnabled(true);
                    encryptHeaders.setEnabled(false);
                    solidMode.setEnabled(false);
                    break;
                case TAR:
                    // no option supported
                    outputArchivePassword.setEnabled(false);
                    compressionLevel.setEnabled(false);
                    encryptHeaders.setEnabled(false);
                    solidMode.setEnabled(false);
                    break;
                case GZ:
                case BZ2:
                case XZ:
                    outputArchivePassword.setEnabled(false);
                    compressionLevel.setEnabled(true);
                    encryptHeaders.setEnabled(false);
                    solidMode.setEnabled(false);
                    break;
            }
        });

        // disable ZIP format (Update error in roothelper)
        if(contentUris != null) {
            archiveTypeSelector.check(R.id._7zRadioButton);
            findViewById(OutputArchiveType.ZIP.getId()).setEnabled(false);

        // LEGACY, java zip backend, set format to ZIP and disable all the settings widgets when in limited content provider mode
//            for (OutputArchiveType t : OutputArchiveType.values())
//                if(t != OutputArchiveType.ZIP)
//                    findViewById(t.getId()).setEnabled(false);
//            outputArchivePassword.setEnabled(false);
//            encryptHeaders.setEnabled(false);
//            compressionLevel.setEnabled(false);
//            solidMode.setEnabled(false);
        }

        // disable streaming archive formats for multiple selection (lazy fail if single directory is passed)
        if(!streamingArchivesEnabled) {
            findViewById(OutputArchiveType.GZ.getId()).setEnabled(false);
            findViewById(OutputArchiveType.BZ2.getId()).setEnabled(false);
            findViewById(OutputArchiveType.XZ.getId()).setEnabled(false);
        }

    }

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

        // Allow only alpha-numeric names. Simplify dealing with reserved path
        // characters.
        if (!FileSaveFragment.IsAlphaNumeric(fileName)) {
            Toast.makeText(this,R.string.alert_bad_filename_chars, Toast.LENGTH_SHORT).show();
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
        if (onCanSave(absolutePath,fileName))
            outputArchiveFilePath.setText(absolutePath +"/"+fileName);
    }

    public void openDestinationArchiveSelector(View unused) {
        // TODO handle Cancel case without useless error messages

        String fragTag = getResources().getString(R.string.tag_fragment_FileSave);

        int idx = archiveTypeSelector.indexOfChild(
                archiveTypeSelector.findViewById(
                        archiveTypeSelector.getCheckedRadioButtonId()));
        String ext = OutputArchiveType.values()[idx].getValue();

        // Get an instance supplying a default extension, captions and
        // icon appropriate to the calling application/activity.
        FileSaveFragment fsf = FileSaveFragment.newInstance(ext,
                android.R.string.ok,
                android.R.string.cancel,
                R.string.app_name,
                R.string.edit_hint,
                R.string.dest_archive_filename_header,
                R.drawable.xfiles_archive);
        fsf.show(getFragmentManager(), fragTag);
    }

    // kill RH if no compress task active and it wasn't main activity the one that launched this activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MainActivity.mainActivity == null) MainActivity.context = null;

        if(!standaloneMode)
            if (MainActivity.mainActivity == null && ProgressIndicator.busy.get() == null)
                MainActivity.killRHWrapper();
    }
}
