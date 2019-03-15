package it.pgp.xfiles.dialogs;


import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.TextView;

import it.pgp.xfiles.R;

public class AboutDialog extends Dialog {

    private void styleIt() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        w.setBackgroundDrawableResource(R.color.transparentBlue);
    }

    public AboutDialog(Context context) {
        super(context);
        styleIt();
        setContentView(R.layout.about_dialog);
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ((TextView)(findViewById(R.id.aboutAppVersionName))).setText(pInfo.versionName);
            ((TextView)(findViewById(R.id.aboutAppVersionCode))).setText(""+pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}