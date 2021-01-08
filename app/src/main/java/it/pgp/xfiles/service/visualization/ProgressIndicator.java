package it.pgp.xfiles.service.visualization;

import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.concurrent.atomic.AtomicReference;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.utils.Pair;

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

    public abstract void setProgress(Pair<Long,Long>... values);

    public void destroy() {
        try{ wm.removeView(oView); } catch(Throwable ignored) {}
        try{ wm.removeView(topLeftView); } catch(Throwable ignored) {}
    }

    protected boolean overlayNotAvailable = false;
    protected WindowManager wm;
    protected LinearLayout oView;
    protected View topLeftView;

    public void addViewToOverlay(View view, WindowManager.LayoutParams params) {
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
