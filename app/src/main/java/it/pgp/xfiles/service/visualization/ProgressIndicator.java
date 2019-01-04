package it.pgp.xfiles.service.visualization;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.concurrent.atomic.AtomicReference;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 21/08/17
 */

public abstract class ProgressIndicator {

    public static final AtomicReference<ForegroundServiceType> busy = new AtomicReference<>(null);

    public static boolean acquire(ForegroundServiceType type) {
        return busy.compareAndSet(null,type);
    }

    public static void release() {
        busy.set(null);
    }

    public abstract void setProgress(Integer... values);

    public void destroy() {
        try {
            if (oView != null) wm.removeView(oView);
            if (topLeftView != null) wm.removeView(topLeftView);
        }
        catch (Exception ignored) {}
    }

    protected boolean overlayNotAvailable = false;
    protected WindowManager wm;
    protected LinearLayout oView;
    protected View topLeftView;

    public void addViewToOverlay(View view, ViewGroup.LayoutParams params) {
        if (overlayNotAvailable) return;
        try {
            wm.addView(view, params);
        }
        catch (Exception e) {
            overlayNotAvailable = true;
            e.printStackTrace();
            MainActivity.showToastOnUIWithHandler("Unable to draw progress bar as system overlay, ensure you have granted overlay permissions");
        }
    }
}
