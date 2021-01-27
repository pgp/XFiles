package it.pgp.xfiles.service.visualization;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicReference;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.utils.Pair;

/**
 * Created by pgp on 21/08/17
 */

public abstract class ProgressIndicator implements View.OnTouchListener {

    public static final AtomicReference<ForegroundServiceType> busy = new AtomicReference<>(null);

    public static boolean acquire(ForegroundServiceType type) {
        return busy.compareAndSet(null,type);
    }

    public static void release() {
        busy.set(null);
    }

    public abstract void setProgress(Pair<Long,Long>... values);

    public void destroy() {
        try{ wm.removeView(oView); } catch(Throwable ignored) {}
        try{ wm.removeView(topLeftView); } catch(Throwable ignored) {}
    }

    protected final Context context;
    protected final WindowManager wm;
    protected View oView;
    protected View topLeftView;

    protected ProgressIndicator(Context context) {
        this.context = context;
        this.wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void addViewToOverlay(View view, WindowManager.LayoutParams params) {
        try {wm.addView(view, params);}
        catch (Exception e) {
            e.printStackTrace();
            MainActivity.showToastOnUIWithHandler("Unable to draw progress bar as system overlay, ensure you have granted overlay permissions");
        }
    }

    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;

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
            if(moving) {
                return true;
            }
        }

        return false;
    }
}
