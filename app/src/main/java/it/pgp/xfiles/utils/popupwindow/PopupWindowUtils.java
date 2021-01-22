package it.pgp.xfiles.utils.popupwindow;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
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
        // 0: reduced, 1: full (lazy init)
        HashView[] hvs = {new HashView(
                activity,
                dataForVisualHash,
                16,3,
                displayMetrics.widthPixels/2,
                displayMetrics.widthPixels/2,
                9), null};

        LayoutInflater layoutInflater = (LayoutInflater)activity.getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.hashview_popup_window, null);

        LinearLayout container = popupView.findViewById(R.id.hvLayout);
        container.addView(hvs[0]);


        TextView htv = popupView.findViewById(R.id.hvTextView);
        TextView tv = popupView.findViewById(R.id.hashview_timeout_alert_textview);

        ((CheckBox)popupView.findViewById(R.id.hvShowFull)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newIndex = isChecked?1:0;
            if(newIndex == 1 && hvs[1] == null)
                hvs[1] = new HashView(
                        activity,
                        dataForVisualHash,
                        16,3,
                        displayMetrics.widthPixels/2,
                        displayMetrics.widthPixels/2);

            HashView currentHv = hvs[1 - newIndex];
            HashView newHv = hvs[newIndex];

            ViewGroup parent = (ViewGroup) currentHv.getParent();
            int targetIndex = parent.indexOfChild(currentHv);
            container.removeView(currentHv);
            container.addView(newHv,targetIndex);
        });


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

    public static void hideSoftKeyBoard(View v) {
        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
