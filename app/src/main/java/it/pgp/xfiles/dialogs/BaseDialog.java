package it.pgp.xfiles.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.view.Window;

public class BaseDialog extends Dialog {
    public BaseDialog(@NonNull Activity activity) {
        super(activity);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
    }

    // https://stackoverflow.com/questions/11430253/how-to-set-icon-for-dialog-in-android
    public void setDialogIcon(int resId) {
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, resId);
    }
}
