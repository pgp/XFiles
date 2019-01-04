package it.pgp.xfiles.dialogs.compress;

import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.BaseDialog;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 19/05/17
 *
 * to be used on open archive (encrypted filenames)
 * (that is, after first failed list archive request to roothelper, with errno set to custom NULL_OR_WRONG_PASSWORD)
 */

public class AskPasswordDialogOnListing extends BaseDialog {
    private ArchivePathContent pendingArchivePath_;

    // after list attempt
    public AskPasswordDialogOnListing(@NonNull final MainActivity activity,
                                      @NonNull final BasePathContent pendingArchivePath) {
        super(activity);
        pendingArchivePath_ = null;
        setTitle("Insert password");
        setContentView(R.layout.ask_password_dialog);
        setDialogIcon(R.drawable.xfiles_archive);

        final EditText password = findViewById(R.id.passwordEditText);
        final CheckBox passwordVisible = findViewById(R.id.passwordVisibleCheckbox);
        final Button ok = findViewById(R.id.askPasswordOkButton);

        passwordVisible.setChecked(false);

        passwordVisible.setOnCheckedChangeListener((buttonView, isChecked) -> password.setInputType(
                passwordVisible.isChecked()?
                        (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        ));

        ok.setOnClickListener(v -> {
            switch (pendingArchivePath.providerType) {
                case LOCAL:
                    pendingArchivePath_ = new ArchivePathContent(pendingArchivePath.dir,"/");
                    break;
                case LOCAL_WITHIN_ARCHIVE:
                    pendingArchivePath_ = new ArchivePathContent(
                            ((ArchivePathContent) pendingArchivePath).archivePath,
                            pendingArchivePath.dir
                    );
                    break;
                default:
                    break;
            }
            pendingArchivePath_.password = password.getText().toString();
            dismiss();
        });

        // cancel is called before dismiss, anyway don't rely on that order
        setOnDismissListener(dialog -> {
            if (pendingArchivePath_ != null) activity.goDir(pendingArchivePath_);
            // wrong password will trigger a new dialog open
        });
    }
}
