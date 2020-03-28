package it.pgp.xfiles.dialogs.compress;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.ExtractService;
import it.pgp.xfiles.service.params.ExtractParams;
import it.pgp.xfiles.utils.FileSelectFragment;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 21/11/17
 */

public class ExtractActivity extends EffectActivity implements FileSelectFragment.Callbacks {

    // TODO should be populated with nearest directory path (parent of archive, or current open directory)
    private LocalPathContent candidateExtractDirectory;
    private boolean isWholeArchiveExtract;

    String filename; // on single selection
    String password;

    private EditText destDirectoryEditText;

    private BasePathContent srcArchiveWithSubDir;

    RadioGroup intermediateDirectoryPolicyRadioGroup; // only enabled when extracting a whole archive (not extracting single items from within the archive)

    private boolean smartDirectoryCreation = false;

    private void getSrcArchiveWithSubDirOrFinish() {
        srcArchiveWithSubDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
        if (srcArchiveWithSubDir.providerType != ProviderType.LOCAL_WITHIN_ARCHIVE &&
                srcArchiveWithSubDir.providerType != ProviderType.LOCAL) {
            Toast.makeText(this,"Unexpected path content type",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (srcArchiveWithSubDir.errorCode != null) {
            Toast.makeText(this,"File ops helper error: "+srcArchiveWithSubDir.errorCode.getValue(),Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        isWholeArchiveExtract = srcArchiveWithSubDir.providerType == ProviderType.LOCAL;
    }

    private BasePathContent getCandidateExtractDirectory() {
        BasePathContent defaultPath = new LocalPathContent(Misc.internalStorageDir.getAbsolutePath());

        if (srcArchiveWithSubDir.errorCode != null) {
            Toast.makeText(this,"File ops helper error: "+srcArchiveWithSubDir.errorCode.getValue(),Toast.LENGTH_SHORT).show();
            return defaultPath;
        }

        switch (srcArchiveWithSubDir.providerType) {
            case LOCAL:
                return srcArchiveWithSubDir;
            case LOCAL_WITHIN_ARCHIVE:
                return new LocalPathContent(((ArchivePathContent)srcArchiveWithSubDir).archivePath).getParent();
            default:
                Toast.makeText(this,"Invalid path type for extraction",Toast.LENGTH_SHORT).show();
                return defaultPath;
        }
    }

    public void extract_ok(View unused) {
        // check if sub items or nothing selected
        List<String> selectedItems = MainActivity.mainActivity.getCurrentBrowserAdapter().getSelectedItemsAsNameOnlyStrings();

        if (selectedItems.size()==0) selectedItems = null;

        // dialog was loaded from context-menu
        if (filename != null) {
            if (srcArchiveWithSubDir.providerType==ProviderType.LOCAL) {
                // context-extract from outside archive, so extract content of archive into dest dir
                srcArchiveWithSubDir = srcArchiveWithSubDir.concat(filename);
            }
            else if (srcArchiveWithSubDir.providerType==ProviderType.LOCAL_WITHIN_ARCHIVE) {
                // context extract from within archive, so extract that file into dest dir
                selectedItems = Collections.singletonList(filename);
            }
            else {
                Toast.makeText(this,"Unexpected path type",Toast.LENGTH_LONG).show();
                return;
            }
        }

        LocalPathContent destDir = new LocalPathContent(destDirectoryEditText.getText().toString());

        if(isWholeArchiveExtract) {
            int idx = intermediateDirectoryPolicyRadioGroup.indexOfChild(
                    intermediateDirectoryPolicyRadioGroup.findViewById(
                            intermediateDirectoryPolicyRadioGroup.getCheckedRadioButtonId()));

            if (idx == 2) { // smart subdirectory creation
                smartDirectoryCreation = true;
            }
            else if (idx == 0) { // always create subdirectory
                String archiveFilename = srcArchiveWithSubDir.getName();
                // strip extension
                int dotIdx = archiveFilename.lastIndexOf('.');
                if(dotIdx <= 0) archiveFilename += ".extracted";
                else archiveFilename = archiveFilename.substring(0,dotIdx);
                destDir = (LocalPathContent) destDir.concat(archiveFilename);
            }
            // else if (idx == 1) {} // nothing to be done for no subdirectory creation
        }

        Intent startIntent = new Intent(MainActivity.mainActivity,ExtractService.class);
        startIntent.setAction(BaseBackgroundService.START_ACTION);
        startIntent.putExtra(
                "params",
                new ExtractParams(
                        srcArchiveWithSubDir,
                        destDir,
                        password,
                        selectedItems,
                        smartDirectoryCreation
                ));
        startService(startIntent);

        finish();
    }

    @Override
    public void onConfirmSelect(String absolutePath, String fileName) {
        destDirectoryEditText.setText(absolutePath);
    }

    @Override
    public boolean isValid(String absolutePath, String fileName) {
        return true;
    }

    public void openDestinationFolderSelector(View unused) {
        String fragTag = getResources().getString(R.string.tag_fragment_FileSelect);

        // Set up a selector for directory selection.
        FileSelectFragment fsf = FileSelectFragment.newInstance(
                FileSelectFragment.Mode.DirectorySelector,
                R.string.alert_OK,
                R.string.alert_cancel,
                R.string.alert_file_select,
                R.drawable.xfiles_new_app_icon,
                R.drawable.xf_dir_blu,
                R.drawable.xfiles_file_icon);

        fsf.show(getFragmentManager(), fragTag);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Extract");
        setActivityIcon(R.drawable.xfiles_extract);
        getSrcArchiveWithSubDirOrFinish();
        Intent intent = getIntent();
        filename = intent.getStringExtra("filename");
        password = intent.getStringExtra("password");
        setContentView(R.layout.extract_layout);
        destDirectoryEditText = findViewById(R.id.extractDirectoryEditText);
        destDirectoryEditText.setText(getCandidateExtractDirectory().dir);
        intermediateDirectoryPolicyRadioGroup = findViewById(R.id.intermediateDirectoryPolicyRadioGroup);
        if(!isWholeArchiveExtract) {
            intermediateDirectoryPolicyRadioGroup.setVisibility(View.GONE);
            findViewById(R.id.intermediateDirectoryPolicyTextView).setVisibility(View.GONE);
        }
    }
}
