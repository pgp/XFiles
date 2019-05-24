package it.pgp.xfiles;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

/**
 * Created by pgp on 03/11/16
 */

public class SwipeRefreshLayoutChildCanScroll extends SwipeRefreshLayout {
    MainActivity mainActivity;

    public SwipeRefreshLayoutChildCanScroll(Context context) {
        super(context);
    }

    public SwipeRefreshLayoutChildCanScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setParentActivity(MainActivity mainActivity) {
        this.mainActivity= mainActivity;
    }

    @Override
    public boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mainActivity.getCurrentMainBrowserView(), -1); // FIXME use static everywhere if possible
    }
}
