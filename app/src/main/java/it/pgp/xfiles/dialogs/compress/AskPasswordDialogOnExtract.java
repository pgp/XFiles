package it.pgp.xfiles.dialogs.compress;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;

import it.pgp.xfiles.R;
import it.pgp.xfiles.dialogs.BaseDialog;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.ExtractService;
import it.pgp.xfiles.service.params.ExtractParams;

/**
 * Created by pgp on 03/06/17
 *
 * to be used on extract
 * (that is, after first failed extract archive request to roothelper, with errno set to custom NULL_OR_WRONG_PASSWORD)
 */

public class AskPasswordDialogOnExtract extends BaseDialog {

    private String pendingPassword;

    // after list attempt
    public AskPasswordDialogOnExtract(@NonNull final Activity activity,
                                      @NonNull final ExtractParams extractParams) {
        super(activity);
        setTitle("Insert password");
        setContentView(R.layout.ask_password_dialog);
        setDialogIcon(R.drawable.xfiles_extract);

        EditText password = findViewById(R.id.passwordEditText);
        CheckedTextView passwordVisible = findViewById(R.id.passwordVisibleCtv);
        Button ok = findViewById(R.id.askPasswordOkButton);

        passwordVisible.setOnClickListener(AskPasswordDialogOnListing.getPasswordCtvListener(password));

        ok.setOnClickListener(v -> {
            pendingPassword = password.getText().toString();
            dismiss();
        });

        // cancel is called before dismiss, anyway don't rely on that order
        setOnDismissListener(dialog -> {
            if(pendingPassword != null) {
                // wrong password will trigger a new dialog open
                // with service & task
                ////////////////////////
                Intent startIntent = new Intent(activity,ExtractService.class);
                startIntent.setAction(BaseBackgroundService.START_ACTION);
                extractParams.setPassword(pendingPassword);
                startIntent.putExtra("params",extractParams);
                activity.startService(startIntent);
                ////////////////////////
                dismiss();
            }
        });
    }

    // after extract attempt
}
