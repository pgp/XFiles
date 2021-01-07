package it.pgp.xfiles.service.visualization;

/**
 * Created by pgp on 10/07/17
 * Overlay progress bar (in addition to the foreground service notification)
 */

import android.app.Service;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MovingRibbon extends ProgressIndicator implements View.OnTouchListener{

    public ProgressBar pb;

    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;

    public MovingRibbon(final Service service, final WindowManager wm) {
        this.wm = wm;
        oView = new LinearLayout(service);
        oView.setBackgroundColor(0x88174a6e); // Semi-transparent dark blue
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                100,
                ViewType.OVERLAY_WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        pb = new ProgressBar(service,null,android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setIndeterminate(false);
        pb.setLayoutParams(params);
        oView.addView(pb);

        oView.setOnTouchListener(this);

        // wm.addView(oView, params);
        addViewToOverlay(oView,params);

        topLeftView = new View(service);
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                ViewType.OVERLAY_WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.START | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;

//        wm.addView(topLeftView,topLeftParams);
        addViewToOverlay(topLeftView,topLeftParams);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();

            moving = false;

            int[] location = new int[2];
            v.getLocationOnScreen(location);

            originalXPos = location[0];
            originalYPos = location[1];

            offsetX = originalXPos - x;
            offsetY = originalYPos - y;

        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

//            Log.i("onTouch","topLeftY="+topLeftLocationOnScreen[1]);
//            Log.i("onTouch","originalY="+originalYPos);

            float x = event.getRawX();
            float y = event.getRawY();

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(v, params);
            moving = true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (moving) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setProgress(Integer... values) {
        pb.setProgress(values[0]);
    }

    @Override
    public void destroy() {
        try{ wm.removeView(oView); } catch(Throwable ignored) {}
        try{ wm.removeView(topLeftView); } catch(Throwable ignored) {}
    }
}
