package it.pgp.xfiles.utils.legacy;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.HashView;
import it.pgp.xfiles.utils.Misc;

/**
 * Created by pgp on 22/12/17
 */

@Deprecated
public class HashViewDialog extends Dialog {

    public HashViewDialog(@NonNull Activity context, byte[] dataForVisualHash, boolean autocloseAfterTimeout) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.hashview_dialog);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        // using 50% of screen width
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        HashView hv = new HashView(
                context,
                dataForVisualHash,
                16,3,
                displayMetrics.widthPixels/2,
                displayMetrics.widthPixels/2);

        hv.setOnClickListener(this::disableDismissTimeout);

        Button cb = findViewById(R.id.hashview_close_button);
        cb.setOnClickListener(v -> dismiss());

        LinearLayout hvLayout = findViewById(R.id.hvLayout);
        hvLayout.removeAllViews();
        hvLayout.addView(hv);

        // postDelayed handler with boolean timeout disable check
        // on no click, dismiss after 5 seconds, on click don't dismiss till click on close button
        TextView htv = findViewById(R.id.hvTextView);
        TextView tv = findViewById(R.id.hashview_timeout_alert_textview);
        if (autocloseAfterTimeout) {
            htv.setVisibility(View.GONE);
            new Handler().postDelayed(this::dynamicDismiss, 5000);
        }
        else {
            tv.setVisibility(View.GONE);
            htv.setText(Misc.toHexString(dataForVisualHash));
        }
    }

    private void disableDismissTimeout(View unused) {
        currentDismissChoice.set(false);
    }
    private final AtomicBoolean currentDismissChoice = new AtomicBoolean(true);
    private void dynamicDismiss() {
        if (currentDismissChoice.get()) dismiss();
    }
}
