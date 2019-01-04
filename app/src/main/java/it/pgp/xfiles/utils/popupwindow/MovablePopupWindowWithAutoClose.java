package it.pgp.xfiles.utils.popupwindow;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import java.util.concurrent.atomic.AtomicBoolean;

public class MovablePopupWindowWithAutoClose extends PopupWindow {

    private GestureDetector gestureDetector;

    private void makeMovable(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            int orgX, orgY;
            int offsetX, offsetY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    disableDismissTimeout();
                    return true;
                } else {
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
            }});
    }



    public MovablePopupWindowWithAutoClose(View contentView, int width, int height, Context context) {
        super(contentView, width, height);
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());
        makeMovable(contentView);
    }

    // migrated from HashViewDialog
    public void disableDismissTimeout() {
        currentDismissChoice.set(false);
    }
    private final AtomicBoolean currentDismissChoice = new AtomicBoolean(true);
    public void dynamicDismiss() {
        if (currentDismissChoice.get()) dismiss();
    }


    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }
}
