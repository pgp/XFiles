package it.pgp.xfiles.dialogs.compress;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.OutputArchiveType;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.CompressService;
import it.pgp.xfiles.service.params.CompressParams;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 19/05/17
 * Deprecated, {@link CompressActivity} is used instead
 */

@Deprecated
public class CompressDialog extends Dialog implements View.OnClickListener /*implements FileSaveFragment.Callbacks*/ {
    private final MainActivity activityContext;
    private final String filename;

    private EditText outputArchiveFilePath;
    private EditText outputArchivePassword;

    private SeekBar compressionLevel;
    private TextView compressionLevelNum;
    private CheckBox encryptHeaders;
    private CheckBox solidMode;


    private ImageButton selectOutputArchiveFilePath;
    private RadioGroup archiveTypeSelector;
    private Button okButton;

    // the nullable filename param is for compressing a single item
    public CompressDialog(@NonNull final MainActivity activityContext, @Nullable String filename) {
        super(activityContext);
        this.activityContext = activityContext;
        this.filename = filename;
        setTitle("Compress");
        setContentView(R.layout.compress_layout);
        outputArchiveFilePath = findViewById(R.id.outputArchiveFilePath);
        outputArchiveFilePath.setText("archive");
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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        encryptHeaders = findViewById(R.id.encryptFilenames);
        solidMode = findViewById(R.id.solidMode);
        solidMode.setChecked(true);

        selectOutputArchiveFilePath = findViewById(R.id.selectOutputArchiveFilePath);
//        selectOutputArchiveFilePath.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openOutputArchiveSelector();
//            }
//        });
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
            }
        });

        okButton = findViewById(R.id.compressDialogOKButton);
        okButton.setOnClickListener(this);

        // refresh performed by onPostExecute of task
//        setOnDismissListener(new OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                activityContext.showDirContent(activityContext.getCurrentDirCommander().refresh());
//            }
//        });
    }

    @Override
    public void onClick(View v) {
        BasePathContent dirPath = activityContext.getCurrentDirCommander().getCurrentDirectoryPathname();
        if (dirPath.providerType != ProviderType.LOCAL) {
            Toast.makeText(activityContext,"Create archive supported only on local filesystem",Toast.LENGTH_SHORT).show();
            return;
        }
        BasePathContent filePath = dirPath.concat(outputArchiveFilePath.getText().toString());
        int idx = archiveTypeSelector.indexOfChild(
                        archiveTypeSelector.findViewById(
                        archiveTypeSelector.getCheckedRadioButtonId()));
        String ext = OutputArchiveType.values()[idx].getValue();

        String destArchive = filePath.toString()+"."+ext;
        String srcFolder = dirPath.toString();

        List<String> selectedItems = activityContext.getCurrentBrowserAdapter().getSelectedItemsAsNameOnlyStrings();

        // compress one single item, only if no other item is selected
        if (selectedItems.size()==0 && filename != null)
            selectedItems = Collections.singletonList(filename);

//        try {
//            MainActivity.currentHelper.compressToArchive(
//                    new LocalPathContent(srcFolder),
//                    new LocalPathContent(destArchive),
//                    selectedItems);
//            Toast.makeText(context,"Compress completed",Toast.LENGTH_SHORT).show();
//            dismiss();
//        }
//        catch (IOException e) {
//            Toast.makeText(context,"Unable to compress: "+e.getMessage(),Toast.LENGTH_SHORT).show();
//        }

        // with service & task
        ////////////////////////
        Intent startIntent = new Intent(activityContext,CompressService.class);
        startIntent.setAction(BaseBackgroundService.START_ACTION);
        startIntent.putExtra(
                "params",
                new CompressParams(
                        new LocalPathContent(srcFolder),
                        new LocalPathContent(destArchive),
                        compressionLevel.getProgress(),
                        encryptHeaders.isChecked(),
                        solidMode.isChecked(),
                        outputArchivePassword.getText().toString(),
                        selectedItems,
                        false
                ));
        activityContext.startService(startIntent);
        ////////////////////////
        dismiss();
    }

//    public void openOutputArchiveSelector() {
//        // TODO handle Cancel case without useless error messages
//
//        String fragTag = activityContext.getResources().getString(R.string.tag_fragment_FileSave);
//
//        // Get an instance supplying a default extension, captions and
//        // icon appropriate to the calling application/activity.
//        FileSaveFragment fsf = FileSaveFragment.newInstance(getCurrentExt(),
//                R.string.alert_OK,
//                R.string.alert_cancel,
//                R.string.app_name,
//                R.string.edit_hint,
//                R.string.empty_string,
//                R.drawable.xf_dir_blu);
//        fsf.show(activityContext.getFragmentManager(), fragTag);
//    }

//    @Override
//    public boolean onCanSave(String absolutePath, String fileName) {
//
//        // Catch the really stupid case.
//        if (absolutePath == null || absolutePath.length() ==0 ||
//                fileName == null || fileName.length() == 0) {
//            Toast.makeText(context, R.string.alert_supply_filename, Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        // Do we have a filename if the extension is thrown away?
//
//        String copyName = FileSaveFragment.NameNoExtension(fileName);
//        if (copyName == null || copyName.length() == 0 ) {
//            Toast.makeText(context,R.string.alert_supply_filename, Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        // Allow only alpha-numeric names. Simplify dealing with reserved path
//        // characters.
//        if (!FileSaveFragment.IsAlphaNumeric(fileName)) {
//            Toast.makeText(context,R.string.alert_bad_filename_chars, Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        // No overwrite of an existing file.
//
//        if (FileSaveFragment.FileExists(absolutePath, fileName)) {
//            Toast.makeText(context,R.string.alert_file_exists, Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        return true;
//    }
//
//    @Override
//    public void onConfirmSave(String absolutePath, String fileName) {
//        if (onCanSave(absolutePath,fileName)) {
//        }
//    }
}
