package it.pgp.xfiles.dialogs;


import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.widget.TextView;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

public class AboutDialog extends Dialog {

    public AboutDialog(MainActivity activity) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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