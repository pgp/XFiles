package it.pgp.xfiles.adapters;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.SelectImageButtonListener;

public class OperationalPagerAdapter extends PagerAdapter {

    private final MainActivity mainActivity;
    private final int[] operationalLayouts;
    private final SelectImageButtonListener l;

    public OperationalPagerAdapter(MainActivity mainActivity, int[] operationalLayouts) {
        this.mainActivity = mainActivity;
        this.operationalLayouts = operationalLayouts;
        this.l = new SelectImageButtonListener(this.mainActivity, R.color.imagebuttonselect);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ViewPager viewPager = (ViewPager) container;
        ViewGroup layout = (ViewGroup) LayoutInflater.from(mainActivity).inflate(operationalLayouts[position], viewPager,false);
        viewPager.addView(layout);
        viewPager.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                // the children listener might not catch the ACTION_UP event due to a too rapid swipe, so restore the involved ImageButton state from here
                if(l.startV != null) {
                    l.startV.getDrawable().clearColorFilter();
                    l.startV.invalidate();
                }
            }
            return false;
        });
        mainActivity.makeImageButtonsStateful(layout, l);
        return layout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return operationalLayouts.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==object;
    }
}
