package it.pgp.xfiles.dialogs;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

public class CreateFileOrDirectoryDialog extends BaseDialog implements Runnable {

    EditText filename;
    Button ok;

    LinearLayout advancedOptionsLayoutToggler; // disabled for folders
    CheckBox advancedOptionsCheckbox;

    LinearLayout advancedOptionsLayout; // disabled for folder, togglable for files

    EditText fileSize;
    RadioGroup fileCreationStrategy;
    final MainActivity mainActivity;
    final FileMode type;

    public CreateFileOrDirectoryDialog(final MainActivity mainActivity, final FileMode type) {
        super(mainActivity);
        this.mainActivity = mainActivity;
        this.type = type;
        setContentView(R.layout.create_file_dir_dialog);
        setDialogIcon(type == FileMode.FILE ?
                R.drawable.xf_new_file :
                R.drawable.xf_new_dir);

        advancedOptionsLayoutToggler = findViewById(R.id.fileDirCreate_advancedOptionsLayoutToggler);
        advancedOptionsCheckbox = findViewById(R.id.fileDirCreate_advancedOptionsCheckbox);
        advancedOptionsLayout = findViewById(R.id.fileDirCreate_advancedOptionsLayout);
        advancedOptionsLayout.setVisibility(View.GONE);

        switch (type) {
            case DIRECTORY:
                setTitle("Create directory");
                advancedOptionsLayoutToggler.setVisibility(View.GONE);
                break;
            case FILE:
                setTitle("Create file");
                advancedOptionsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> advancedOptionsLayout.setVisibility(isChecked?View.VISIBLE:View.GONE));
                fileSize = findViewById(R.id.fileDirCreate_fileSize);
                fileCreationStrategy = findViewById(R.id.fileDirCreate_fileCreationStrategy);
                break;
            default:
                throw new RuntimeException("Undefined file mode"); // Unreachable statement
        }

        filename = findViewById(R.id.fileDirCreate_filename);
        ok = findViewById(R.id.fileDirCreate_OkButton);

        ok.setOnClickListener(v -> {
            toggleButtons(true);
            new Thread(this).start();
        });
    }

    private void toggleButtons(boolean start) {
        setCancelable(!start);
        ok.setEnabled(!start);
        ok.setText(start?"Creating...":"OK");
    }

    @Override
    public void run() {
        final String filename_ = filename.getText().toString();
        if (filename_.contains("/")) {
            MainActivity.showToastOnUI("Full path creation implemented but not enabled yet");
            toggleButtons(false);
            return;
        }
        if (filename_.isEmpty()) {
            MainActivity.showToastOnUI("Empty filename provided");
            toggleButtons(false);
            return;
        }

        BasePathContent f = mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname().concat(filename_);

        // dirty hack to workaround final variable requirements in lambdas and catch-finally data flow dependency
        // Collection.singletonList or Arrays.asList cannot be used here, in that they create immutables
        List<String> nameToLocate = new ArrayList<String>(){{add(filename_);}};

        try {
            int idx = (advancedOptionsCheckbox.isChecked() && type == FileMode.FILE)?
                    1 + fileCreationStrategy.indexOfChild(
                            fileCreationStrategy.findViewById(
                                    fileCreationStrategy.getCheckedRadioButtonId())):
                    -1;
            FileCreationAdvancedOptions[] opts = (idx == -1)?new FileCreationAdvancedOptions[]{}:
                    new FileCreationAdvancedOptions[]{
                            new FileCreationAdvancedOptions(
                                    fileSize.getText().toString().isEmpty()?
                                            0:
                                            Long.parseLong(fileSize.getText().toString())
                                    , FileCreationAdvancedOptions.CreationStrategy.values()[idx])
                    };

            if (f instanceof RemotePathContent) {
                MainActivity.sftpProvider.createFileOrDirectory(f,type);
            }
            else {
                MainActivity.getRootHelperClient().createFileOrDirectory(f,type,opts);
            }
            MainActivity.showToastOnUI(type.name().toLowerCase()+" created");
        }
        catch (IOException e) {
            e.printStackTrace();
            MainActivity.showToastOnUI("File creation error, reason: "+e.getMessage());
            nameToLocate.clear();
        }
        finally {
            mainActivity.runOnUiThread(()->{
                mainActivity.browserPagerAdapter.showDirContent(mainActivity.getCurrentDirCommander().refresh(),mainActivity.browserPager.getCurrentItem(),
                        nameToLocate.isEmpty()?
                                new String[0]:new String[]{nameToLocate.get(0)});
                dismiss();
            });
        }
    }
}
