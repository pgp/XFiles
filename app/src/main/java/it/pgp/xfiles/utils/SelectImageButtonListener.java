package it.pgp.xfiles.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class SelectImageButtonListener implements View.OnTouchListener {

    private final int color;

    public ImageButton startV;

    public SelectImageButtonListener(Context context, int color) {
        this.color = context.getResources().getColor(color);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof ImageButton) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("ZZZZZZZZZ","down");
                    startV = (ImageButton) v;
                    startV.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    v.invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("ZZZZZZZZZ","up");
                    if (startV != null) {
                        // interceptor view has changed due to a too rapid gesture, clear state on the old view in that case
                        startV.getDrawable().clearColorFilter();
                        startV.invalidate();
                    }
                    if (v != startV) { // should never happen
                        ((ImageButton) v).getDrawable().clearColorFilter();
                        v.invalidate();
                    }
                    startV = null;
                    break;
            }
        }
        return false;
    }
}
