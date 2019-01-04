package it.pgp.xfiles.dialogs.compress;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.ExtractService;
import it.pgp.xfiles.service.params.ExtractParams;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 01/06/17
 * Deprecated, {@link ExtractActivity} is used instead
 */

@Deprecated
public class ExtractDialog extends Dialog {

//    FileOpsErrorCodes errcode;
//    Boolean okClicked;

    private EditText destDirectoryEditText;

    // extract dialog will not contain fields for asking password before extraction (that is, before it is even
    // known that an archive has password; for archive that have listArchive already performed, invoke constructor
    // with password retrieved from archiveMRU's vMap)
    public ExtractDialog(@NonNull final MainActivity activityContext,
                         @Nullable final String password,
                         @Nullable final String filename) {
        super(activityContext);
        setTitle("Extract");
        setContentView(R.layout.single_filename_dialog);
        destDirectoryEditText = findViewById(R.id.singleFilenameEditText);
        Button okButton = findViewById(R.id.singleFilenameOkButton);
        okButton.setOnClickListener(v -> {
            // check if sub items or nothing selected
            List<String> selectedItems = activityContext.getCurrentBrowserAdapter().getSelectedItemsAsNameOnlyStrings();

            BasePathContent srcArchiveWithSubDir = activityContext.getCurrentDirCommander().getCurrentDirectoryPathname();
            if (srcArchiveWithSubDir.providerType != ProviderType.LOCAL_WITHIN_ARCHIVE &&
                    srcArchiveWithSubDir.providerType != ProviderType.LOCAL) {
                Toast.makeText(activityContext,"Unexpected path content type",Toast.LENGTH_SHORT).show();
                return;
            }
            if (srcArchiveWithSubDir.errorCode != null) {
                Toast.makeText(activityContext,"File ops helper error: "+srcArchiveWithSubDir.errorCode.getValue(),Toast.LENGTH_SHORT).show();
                return;
            }

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
                    Toast.makeText(activityContext,"Unexpected path type",Toast.LENGTH_LONG).show();
                    return;
                }
            }

            LocalPathContent destDir = new LocalPathContent(destDirectoryEditText.getText().toString());

            // with service & task
            ////////////////////////
            Intent startIntent = new Intent(activityContext,ExtractService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra(
                    "params",
                    new ExtractParams(
                            srcArchiveWithSubDir,
                            destDir,
                            password,
                            selectedItems
                    ));
            activityContext.startService(startIntent);
            ////////////////////////
            dismiss();

        });
    }
}
