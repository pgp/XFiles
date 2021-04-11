package it.pgp.xfiles.dialogs;


import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.TextView;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

public class AboutDialog extends Dialog {

    private void styleIt() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public AboutDialog(MainActivity activity) {
        super(activity);
        styleIt();
        setContentView(R.layout.about_dialog);
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            ((TextView)(findViewById(R.id.aboutAppVersionName))).setText(pInfo.versionName);
            ((TextView)(findViewById(R.id.aboutAppVersionCode))).setText(""+pInfo.versionCode);
            findViewById(R.id.updateCheckButton).setOnClickListener(v -> {
                dismiss();
                new UpdateCheckDialog(activity).show();
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}