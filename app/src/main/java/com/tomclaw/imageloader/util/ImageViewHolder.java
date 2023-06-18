package com.tomclaw.imageloader.util;

import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.tomclaw.imageloader.core.ViewHolder;
import com.tomclaw.imageloader.core.ViewSize;

import java.util.concurrent.CountDownLatch;

public class ImageViewHolder implements ViewHolder<ImageView> {

    private final ImageView imageView;

    public ImageViewHolder(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    public ViewSize optSize() {
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        int width;
        if(params != null && params.width > 0) {
            width = params.width;
        }
        else if (imageView.getWidth() > 0)
            width = imageView.getWidth();
        else width = ViewGroup.LayoutParams.MATCH_PARENT;

        if(width == ViewGroup.LayoutParams.WRAP_CONTENT)
            width = imageView.getContext().getResources().getDisplayMetrics().widthPixels;


        int height;
        if(params != null && params.height > 0) {
            height = params.height;
        }
        else if (imageView.getHeight() > 0)
            height = imageView.getHeight();
        else height = ViewGroup.LayoutParams.MATCH_PARENT;

        if(height == ViewGroup.LayoutParams.WRAP_CONTENT)
            height = imageView.getContext().getResources().getDisplayMetrics().heightPixels;


        return width > 0 && height > 0 ? new ViewSize(width, height) : null;
    }

    @Override
    public ViewSize getSize() {
        ViewSize optSize = optSize();
        if(optSize != null) return optSize;

        final ViewSize[] viewSize = {null};
        CountDownLatch latch = new CountDownLatch(1);

        ViewTreeObserver viewTreeObserver = imageView.getViewTreeObserver();
        ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {

            private boolean isResumed = false;

            @Override
            public boolean onPreDraw() {
                ViewSize size = optSize();
                if(size != null) {
                    viewSize[0] = size;
                    if(viewTreeObserver.isAlive()) // without this: java.lang.IllegalStateException: This ViewTreeObserver is not alive, call getViewTreeObserver() again
                        viewTreeObserver.removeOnPreDrawListener(this);

                    if(!isResumed) {
                        isResumed = true;
                        latch.countDown();
                    }
                }
                return true;
            }
        };
        viewTreeObserver.addOnPreDrawListener(preDrawListener);

        try {
            latch.await();
        }
        catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        return viewSize[0];
    }

    @Override
    public Object getTag() {
        return imageView.getTag();
    }

    @Override
    public void setTag(Object value) {
        imageView.setTag(value);
    }

    @Override
    public ImageView get() {
        return imageView;
    }
}