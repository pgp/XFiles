package it.pgp.xfiles;

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;

public abstract class EffectActivity extends Activity {

    protected ActionBar b;

    protected void setActivityIcon(int resId) {
        if (b != null) b.setIcon(resId);
    }

    public static Object currentlyOnFocus;

    public static final DialogInterface.OnShowListener defaultDialogShowListener = d -> EffectActivity.currentlyOnFocus = d;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = getActionBar();
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentlyOnFocus = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }
}
