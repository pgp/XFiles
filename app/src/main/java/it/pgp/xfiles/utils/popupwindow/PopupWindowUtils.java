package it.pgp.xfiles.utils.popupwindow;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.HashView;
import it.pgp.xfiles.utils.Misc;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class PopupWindowUtils {

    public static PopupWindow createAndShowHashViewPopupWindow(@NonNull Activity activity,
                                                               byte[] dataForVisualHash,
                                                               boolean autocloseAfterTimeout,
                                                               View anchor) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        // using 50% of screen width
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        HashView hv = new HashView(
                activity,
                dataForVisualHash,
                16,3,
                displayMetrics.widthPixels/2,
                displayMetrics.widthPixels/2);

        LayoutInflater layoutInflater = (LayoutInflater)activity.getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.hashview_popup_window, null);

        LinearLayout container = popupView.findViewById(R.id.hvLayout);
        container.addView(hv);


        TextView htv = popupView.findViewById(R.id.hvTextView);
        TextView tv = popupView.findViewById(R.id.hashview_timeout_alert_textview);


        MovablePopupWindowWithAutoClose popupWindow = new MovablePopupWindowWithAutoClose(
                popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, activity);

        Button btnDismiss = popupView.findViewById(R.id.hashview_close_button);
        btnDismiss.setOnClickListener(view -> popupWindow.dismiss());

        if (autocloseAfterTimeout) {
            htv.setVisibility(View.GONE);
            new Handler().postDelayed(popupWindow::dynamicDismiss, 5000);
        }
        else {
            tv.setVisibility(View.GONE);
            htv.setText(Misc.toHexString(dataForVisualHash));
        }

        popupWindow.showAtLocation(
                anchor,
                Gravity.NO_GRAVITY,
                displayMetrics.widthPixels/4,
                displayMetrics.widthPixels/4);

        return popupWindow;
    }
}
