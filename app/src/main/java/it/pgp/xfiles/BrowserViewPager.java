package it.pgp.xfiles;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Web source:
 * https://stackoverflow.com/questions/16671382/limited-swipe-area-android#16671976
 * for limiting swipe area when in continuous (grid) select mode
 *
 * TODO fix coupling between modes (selection enabled, continuous selection, grid vs list view)
 * example: if select mode is on and switch to grid, padLayout is not full
 * (each one of the three button should update compound mode behaviour accordingly)
 */

public class BrowserViewPager extends ViewPager {

    public BrowserViewPager(Context context) {
        super(context);
    }

    public BrowserViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void update() {
        MainActivity.mainActivity.updateScreenDimensions();
        startX = 0.15f*(MainActivity.mainActivity.currentScreenDimensions.x);
        endX = 0.85f*(MainActivity.mainActivity.currentScreenDimensions.x);
        Log.d(getClass().getName(),"startX: "+startX+" endX: "+endX);
    }

    private float startX,endX;
    public boolean swipeDisabled = false; // true if no continuous (grid) selection enabled

    public void switchNeutralArea() {
        swipeDisabled = !swipeDisabled;
        update();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(swipeDisabled && inNeutralArea(ev.getX(),ev.getY())) {
            //--events re-directed to this ViewPager's onTouch() and to its child views from there--
            return false;
        }
        else {
            //--events intercepted by this ViewPager's default implementation, where it looks for swipe gestures--
            return super.onInterceptTouchEvent(ev);
        }
    }

    // area in which swipe must be disabled
    private boolean inNeutralArea(float x, float y) {
        return x > startX && x < endX; // allow swiping only from screen borders
    }
}
