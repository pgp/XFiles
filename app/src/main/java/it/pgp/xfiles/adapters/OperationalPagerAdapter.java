package it.pgp.xfiles.adapters;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EdgeEffect;

import java.lang.reflect.Field;

import it.pgp.xfiles.MainActivity;

public class OperationalPagerAdapter extends PagerAdapter {

    private final MainActivity context;
    private final int[] operationalLayouts;

    public static void setEdgeGlowColor(ViewPager viewPager, int color) {
        try {
            Class<?> clazz = ViewPager.class;
            for (String name : new String[] {
                    "mLeftEdge", "mRightEdge"
            }) {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                Object edge = field.get(viewPager); // android.support.v4.widget.EdgeEffectCompat
                Field fEdgeEffect = edge.getClass().getDeclaredField("mEdgeEffect");
                fEdgeEffect.setAccessible(true);
                setEdgeEffectColor((EdgeEffect) fEdgeEffect.get(edge), color);
            }
        } catch (Exception ignored) {
        }
    }

    public static void setEdgeEffectColor(EdgeEffect edgeEffect, int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                edgeEffect.setColor(color);
                return;
            }
            Field edgeField = EdgeEffect.class.getDeclaredField("mEdge");
            Field glowField = EdgeEffect.class.getDeclaredField("mGlow");
            edgeField.setAccessible(true);
            glowField.setAccessible(true);
            Drawable mEdge = (Drawable) edgeField.get(edgeEffect);
            Drawable mGlow = (Drawable) glowField.get(edgeEffect);
            mEdge.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            mGlow.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            mEdge.setCallback(null); // free up any references
            mGlow.setCallback(null); // free up any references
        } catch (Exception ignored) {
        }
    }

    public OperationalPagerAdapter(MainActivity mainActivity, int[] operationalLayouts) {
        this.context = mainActivity;
        this.operationalLayouts = operationalLayouts;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ViewPager viewPager = (ViewPager) container;
        ViewGroup layout = (ViewGroup) LayoutInflater.from(context).inflate(operationalLayouts[position], viewPager,false);
        viewPager.addView(layout);
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
