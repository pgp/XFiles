package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;

/**
 * Created by pgp on 26/10/16
 */

public class ImmersiveModeDialog extends Dialog {
    public ImmersiveModeDialog(Context context) {
        super(context);
    }

    @Override
    public void show() {
        // Set the dialog to not focusable.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

//        copySystemUiVisibility();
//        web source: http://stackoverflow.com/questions/22794049/how-to-maintain-the-immersive-mode-in-dialogs

        // Show the dialog with NavBar hidden.
        super.show();

        // Set the dialog to focusable again.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
