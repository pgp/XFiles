package it.pgp.xfiles.sftpclient;

import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Toast;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.dialogs.SSHAlreadyInKnownHostsDialog;
import it.pgp.xfiles.dialogs.SSHNotInKnownHostsDialog;

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
        SSHNotInKnownHostsDialog nd;
        SSHAlreadyInKnownHostsDialog ad;

        if (dialog instanceof SSHNotInKnownHostsDialog) {
            nd = (SSHNotInKnownHostsDialog) dialog;

            // TODO refactor common superclass out of SSHAlreadyInKnownHostsDialog and SSHNotInKnownHostsDialog
            // re-do ls request
            if (nd.pendingLsPath == null) {
                Toast.makeText(activity.getApplicationContext(),"No pending request in SFTP retry",Toast.LENGTH_LONG).show();
                return;
            }

            activity.goDir(nd.pendingLsPath);
        }
        else if (dialog instanceof SSHAlreadyInKnownHostsDialog) {
            ad = (SSHAlreadyInKnownHostsDialog) dialog;

            if (ad.pendingLsPath == null) {
                Toast.makeText(activity.getApplicationContext(),"No pending request in SFTP retry",Toast.LENGTH_LONG).show();
                return;
            }

            activity.goDir(ad.pendingLsPath);
        }
    }
}
