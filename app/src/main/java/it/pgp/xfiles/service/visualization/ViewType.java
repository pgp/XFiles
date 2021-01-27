package it.pgp.xfiles.service.visualization;

import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

public enum ViewType {

    CONTAINER,
    ANCHOR,
    CONTAINER_WRAP;

    public static final int OVERLAY_WINDOW_TYPE = (Build.VERSION.SDK_INT < 26)?
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT:
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

    static final Map<ViewType, WindowManager.LayoutParams> m;

    static {
        m = new HashMap<>();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                200,
                OVERLAY_WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        m.put(CONTAINER,params);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;

        m.put(ANCHOR,params);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_WINDOW_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        m.put(CONTAINER_WRAP,params);
    }

    public WindowManager.LayoutParams getParams() {
        return m.get(this);
    }
}
