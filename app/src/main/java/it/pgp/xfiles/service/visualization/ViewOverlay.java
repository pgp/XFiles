package it.pgp.xfiles.service.visualization;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.utils.Pair;
import it.pgp.xfiles.utils.popupwindow.PopupWindowUtils;

// actually NOT a ProgressIndicator, only reusing overlay logic, maybe ProgressIndicator itself should be renamed
public class ViewOverlay extends ProgressIndicator implements View.OnTouchListener {

    private GestureDetector gestureDetector;

    public ViewOverlay(Context context,
                       View builtView) {
        super(context);
        builtView.setOnTouchListener(this);
        this.oView = builtView;
        topLeftView = new View(context);
        MainActivity.handler.post(()->{
            gestureDetector = new GestureDetector(context, PopupWindowUtils.singleTapConfirm);
            addViewToOverlay(oView, ViewType.CONTAINER_WRAP.getParams());
            addViewToOverlay(topLeftView, ViewType.ANCHOR.getParams());
        });
    }

    public final AutoDismissControl adc = new AutoDismissControl(this::destroy);

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            adc.disableDismissTimeout();
            return true;
        }

        return super.onTouch(v, event);
    }

    @Override
    public void setProgress(Pair<Long,Long>... values) {}
}
