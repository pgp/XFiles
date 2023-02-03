package it.pgp.xfiles.dialogs;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.CreateFileService;
import it.pgp.xfiles.service.params.CreateFileParams;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.SFTPPathContent;
import it.pgp.xfiles.utils.pathcontent.SMBPathContent;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

public class CreateFileOrDirectoryDialog extends BaseDialog {

    EditText filename;
    Button ok;

    CheckedTextView advancedOptionsCtv; // disabled for folders

    LinearLayout advancedOptionsLayout; // disabled for folder, togglable for files

    EditText fileSize;
    RadioGroup fileCreationStrategy, sizeUnit;
    EditText prngSeed;
    final MainActivity mainActivity;
    final FileMode type;
    long byteMultiplier = 1;

    public CreateFileOrDirectoryDialog(final MainActivity mainActivity, final FileMode type, boolean showAdvancedOptions, String currentState) {
        super(mainActivity);
        this.mainActivity = mainActivity;
        this.type = type;
        setContentView(R.layout.create_file_dir_dialog);
        setDialogIcon(type == FileMode.FILE ?
                R.drawable.xf_new_file :
                R.drawable.xf_new_dir);

        advancedOptionsCtv = findViewById(R.id.fileDirCreate_advancedOptionsCtv);
        advancedOptionsLayout = findViewById(R.id.fileDirCreate_advancedOptionsLayout);
        advancedOptionsLayout.setVisibility(View.GONE);

        switch (type) {
            case DIRECTORY:
                setTitle("Create directory");
                advancedOptionsCtv.setVisibility(View.GONE);
                break;
            case FILE:
                setTitle("Create file");
                advancedOptionsCtv.setOnClickListener(v -> {
                    advancedOptionsCtv.toggle();
                    advancedOptionsLayout.setVisibility(advancedOptionsCtv.isChecked() ? View.VISIBLE : View.GONE);
                });
                fileSize = findViewById(R.id.fileDirCreate_fileSize);
                fileCreationStrategy = findViewById(R.id.fileDirCreate_fileCreationStrategy);
                sizeUnit = findViewById(R.id.sizeUnitRadioGroup);
                prngSeed = findViewById(R.id.fileCreationStrategy_seed);
                break;
            default:
                throw new RuntimeException("Undefined file mode"); // Unreachable statement
        }

        filename = findViewById(R.id.fileDirCreate_filename);
        filename.setText(currentState);
        ok = findViewById(R.id.fileDirCreate_OkButton);

        ok.setOnClickListener(v -> {
            final String filename_ = filename.getText().toString();
            if (filename_.contains("/")) {
                MainActivity.showToast("Full path creation implemented but not enabled yet");
                return;
            }
            if (filename_.isEmpty()) {
                MainActivity.showToast("Empty filename provided");
                return;
            }
            toggleButtons(true);

            BasePathContent f = mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname().concat(filename_);
            // dirty hack to workaround final variable requirements in lambdas and catch-finally data flow dependency
            // Collection.singletonList or Arrays.asList cannot be used here, in that they create immutables
            List<String> nameToLocate = new ArrayList<String>(){{add(filename_);}};

            try {
                String tmp = fileSize.getText().toString();
                long fileSizeL = 0;
                if(!tmp.isEmpty()) {
                    int idx = sizeUnit.indexOfChild(sizeUnit.findViewById(sizeUnit.getCheckedRadioButtonId()));
                    switch(idx) {
                        case 0:
                            byteMultiplier = 1L;
                            break;
                        case 1:
                            byteMultiplier = 1000L;
                            break;
                        case 2:
                            byteMultiplier = 1000000L;
                            break;
                        case 3:
                            byteMultiplier = 1000000000L;
                            break;
                        default:
                            throw new RuntimeException("Unsupported size unit");
                    }
                    fileSizeL = Long.parseLong(tmp)*byteMultiplier;
                }
                int idx = (advancedOptionsCtv.isChecked() && type == FileMode.FILE)?
                        1 + fileCreationStrategy.indexOfChild(
                                fileCreationStrategy.findViewById(
                                        fileCreationStrategy.getCheckedRadioButtonId())):
                        -1;
                FileCreationAdvancedOptions.CreationStrategy targetStrategy = (idx == -1) ? null : FileCreationAdvancedOptions.CreationStrategy.values()[idx];
                FileCreationAdvancedOptions opts = (idx == -1) ? null : new FileCreationAdvancedOptions(fileSizeL, targetStrategy);
                if(targetStrategy == FileCreationAdvancedOptions.CreationStrategy.RANDOM_CUSTOM_SEED) opts.seed = prngSeed.getText().toString();

                if(f instanceof SFTPPathContent) MainActivity.sftpProvider.createFileOrDirectory(f,type);
                else if(f instanceof SMBPathContent) MainActivity.smbProvider.createFileOrDirectory(f,type);
                else {
//                    MainActivity.getRootHelperClient().createFileOrDirectory(f,type,opts);
                    try {
                        Intent startIntent = new Intent(mainActivity, CreateFileService.class);
                        startIntent.setAction(BaseBackgroundService.START_ACTION);
                        startIntent.putExtra("params", new CreateFileParams(f, opts));
                        mainActivity.startService(startIntent);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    dismiss();
                    return;
                }
                MainActivity.showToast(type.name().toLowerCase()+" created");
            }
            catch(Exception e) {
                e.printStackTrace();
                MainActivity.showToast(e.getMessage());
                nameToLocate.clear();
            }

            mainActivity.runOnUiThread(()->{
                mainActivity.browserPagerAdapter.showDirContent(
                        mainActivity.getCurrentDirCommander().refresh(),
                        mainActivity.browserPager.getCurrentItem(),
                        nameToLocate.isEmpty()?null:nameToLocate.get(0));
                dismiss();
            });
        });

        if(type==FileMode.FILE && showAdvancedOptions) advancedOptionsCtv.performClick();
    }

    private void toggleButtons(boolean start) {
        setCancelable(!start);
        ok.setEnabled(!start);
        ok.setText(start?"Creating...":"OK");
    }

    public static boolean doCreate(MainActivity mainActivity, BasePathContent ff, FileMode type) {
        try {
            mainActivity.getFileOpsHelper(ff.providerType).createFileOrDirectory(ff,type);
            MainActivity.showToast(type.name().toLowerCase()+" created");
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            MainActivity.showToast("Error creating "+type.name().toLowerCase()+": "+e.getMessage());
            return false;
        }
    }

    public static void resetCreateMode(BrowserAdapter ba, AbsListView lv) {
        if(lv instanceof ListView) {
            if(ba != null && ba.fastCreateModeHeaderView != null) {
                ((ListView) lv).removeHeaderView(ba.fastCreateModeHeaderView);
                ba.fastCreateModeHeaderView = null;
            }
        }
    }

    public static void toggleFastCreateMode(MainActivity mainActivity, FileMode type, boolean status) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        EditText et;
        ListView listView = (ListView) mainActivity.browserPagerAdapter.mainBrowserViews[mainActivity.browserPager.getCurrentItem()];
        // DO NOT call listView.getAdapter(), which returns an adapter wrapper when fast create mode is enabled
        BrowserAdapter ba = mainActivity.getCurrentBrowserAdapter();
        if(status) { // -> ON
            if(ba.fastCreateModeHeaderView != null) {
                MainActivity.showToast("You are already creating a file or directory");
                et = ba.fastCreateModeHeaderView.findViewById(R.id.browserItemFilename_edit);
                listView.setSelection(0);
                et.requestFocus();
                PopupWindowUtils.toggleSoftKeyBoard(et, true);
                return;
            }
            ba.fastCreateModeHeaderView = inflater.inflate(R.layout.browser_item, null);
            ba.fastCreateModeHeaderView.findViewById(R.id.browserItemFilename).setVisibility(View.GONE);
            et = ba.fastCreateModeHeaderView.findViewById(R.id.browserItemFilename_edit);
            et.setVisibility(View.VISIBLE);
            et.setOnEditorActionListener((v, actionId, event) -> {
                // If the event is a key-down event on the "enter" button
                if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    // Perform action on key press
                    toggleFastCreateMode(mainActivity, type, false);
                    return true;
                }
                return false;
            });
            ImageView iv = ba.fastCreateModeHeaderView.findViewById(R.id.fileTypeImage);
            if(type==FileMode.FILE) {
                iv.setImageResource(android.R.drawable.ic_dialog_info);
                iv.setOnClickListener(v -> {
                    String content = et.getText().toString();
                    et.clearFocus();
                    resetCreateMode(ba, listView);
                    new CreateFileOrDirectoryDialog(mainActivity, type, true, content).show();
                });
            }
            else iv.setImageBitmap(BrowserAdapter.dirIV);

            listView.addHeaderView(ba.fastCreateModeHeaderView);
            listView.setSelection(0);
            et.requestFocus();
            PopupWindowUtils.toggleSoftKeyBoard(et, true);
        }
        else { // -> OFF
            if(ba.fastCreateModeHeaderView != null) {
                et = ba.fastCreateModeHeaderView.findViewById(R.id.browserItemFilename_edit);
                final String filename_ = et.getText().toString();
                BasePathContent ff = mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname().concat(filename_);

                listView.removeHeaderView(ba.fastCreateModeHeaderView);
                ba.fastCreateModeHeaderView = null;
                et.clearFocus(); // fixes "parameter must be a descendant of this view" on legacy Android versions

                if(doCreate(mainActivity, ff, type))
                    mainActivity.runOnUiThread(()-> mainActivity.browserPagerAdapter.showDirContent(
                            mainActivity.getCurrentDirCommander().refresh(),
                            mainActivity.browserPager.getCurrentItem(),
                            filename_));
            }

            View v = mainActivity.getCurrentFocus();
            if (v != null) PopupWindowUtils.toggleSoftKeyBoard(v, false);
        }
    }
}
