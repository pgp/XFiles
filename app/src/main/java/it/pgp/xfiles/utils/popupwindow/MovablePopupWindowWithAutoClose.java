package it.pgp.xfiles.utils.popupwindow;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import it.pgp.xfiles.service.visualization.AutoDismissControl;

public class MovablePopupWindowWithAutoClose extends PopupWindow {

    private final GestureDetector gestureDetector;

    int orgX, orgY;
    int offsetX, offsetY;

    private void makeMovable(View view) {
        view.setOnTouchListener((v,event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                adc.disableDismissTimeout();
                return true;
            }
            else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        orgX = (int) event.getX();
                        orgY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        offsetX = (int)event.getRawX() - orgX;
                        offsetY = (int)event.getRawY() - orgY;
                        update(offsetX, offsetY, -1, -1, true);
                        break;
                }
            }
            return true;
        });
    }

    public MovablePopupWindowWithAutoClose(View contentView, int width, int height, Context context) {
        super(contentView, width, height);
        gestureDetector = new GestureDetector(context, PopupWindowUtils.singleTapConfirm);
        makeMovable(contentView);
    }

    public final AutoDismissControl adc = new AutoDismissControl(this::dismiss);
}
