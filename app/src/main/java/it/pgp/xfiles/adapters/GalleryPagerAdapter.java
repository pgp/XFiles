package it.pgp.xfiles.adapters;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.util.ArrayList;

import it.pgp.xfiles.R;


public class GalleryPagerAdapter extends PagerAdapter {

    private final boolean showOnlyCurrentImage;
    private Activity activity;
    private boolean isShowing = true;
    private HorizontalScrollView toolbar;
    private RecyclerView imagesHorizontalList;
    private ArrayList<String> imageList;

    public GalleryPagerAdapter(Activity activity, ArrayList<String> dataSet, HorizontalScrollView toolbar, RecyclerView imagesHorizontalList, boolean showOnlyCurrentImage) {
        this.activity = activity;
        this.imageList = dataSet;
        this.toolbar = toolbar;
        this.imagesHorizontalList = imagesHorizontalList;
        this.showOnlyCurrentImage = showOnlyCurrentImage;
    }

    @Override
    public int getCount() {
        return imageList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = LayoutInflater.from(activity).inflate(R.layout.pager_item, container, false);
        SubsamplingScaleImageView imageView = itemView.findViewById(R.id.iv);
        imageView.setImage(ImageSource.uri(imageList.get(position)));
        TextView filename = itemView.findViewById(R.id.pager_item_filename);
        filename.setText(imageList.get(position));

        if(!showOnlyCurrentImage) {
            GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                    if (isShowing) {
                        toolbar.animate().translationY(-toolbar.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                        imagesHorizontalList.animate().translationY(imagesHorizontalList.getBottom()).setInterpolator(new AccelerateInterpolator()).start();
                        filename.setVisibility(View.GONE);
                    } else {
                        toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                        imagesHorizontalList.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                        filename.setVisibility(View.VISIBLE);
                    }
                    isShowing = !isShowing;
                    return false;
                }
            });
            imageView.setOnTouchListener((view, motionEvent) -> gestureDetector.onTouchEvent(motionEvent));
        }

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }
}
