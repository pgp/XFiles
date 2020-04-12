package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Toast;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.SSHKnownHostsBaseDialog;

/**
 * Created by pgp on 09/03/17
 *
 * onDismiss() is always fired when dialog closes. The documentation for setOnCancelListener() states: "This will only be invoked when the dialog is canceled, if the creator needs to know when it is dismissed in general, use setOnDismissListener", i.e. IT'S NOT EITHER ONCANCEL OR ONDISMISS BUT BOTH WHEN A DIALOG IS CANCELED. I agree though that it would have made more sense had that not been the case.
 *
 * web source: http://stackoverflow.com/questions/8303330/android-dialog-dismisses-instead-of-cancel
 */

public class SftpRetryLsListener implements Dialog.OnDismissListener {

    MainActivity activity;

    public SftpRetryLsListener(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        SSHKnownHostsBaseDialog d = (SSHKnownHostsBaseDialog)dialog;
        if (d.pendingLsPath == null) {
            Toast.makeText(activity.getApplicationContext(),"No pending request in SFTP retry",Toast.LENGTH_LONG).show();
            return;
        }
        activity.goDir(d.pendingLsPath,null);
    }
}
