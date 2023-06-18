package com.tomclaw.imageloader.util;

import android.graphics.PorterDuff;
import android.widget.ImageView;

import com.tomclaw.imageloader.core.Handlers;
import com.tomclaw.imageloader.core.ViewHolder;

public class ImageViewHandlers {
    // these are all ported from extension functions

    public static Handlers<ImageView> fitCenter(Handlers<ImageView> handlers) {
        handlers.success = (viewHolder, result) -> {
            ImageView imgView = viewHolder.get();
            imgView.setImageDrawable(null);
            imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imgView.setColorFilter(null);
            imgView.setImageDrawable(result.getDrawable());
        };
        return handlers;
    }

    public static Handlers<ImageView> matrixMode(Handlers<ImageView> handlers) {
        handlers.success = (viewHolder, result) -> {
            ImageView imgView = viewHolder.get();
            imgView.setImageDrawable(null);
            imgView.setScaleType(ImageView.ScaleType.MATRIX);
            imgView.setColorFilter(null);
            imgView.setImageDrawable(result.getDrawable());
        };
        return handlers;
    }

    public static Handlers<ImageView> centerCrop(Handlers<ImageView> handlers) {
        handlers.success = (viewHolder, result) -> {
            ImageView imgView = viewHolder.get();
            imgView.setImageDrawable(null);
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgView.setColorFilter(null);
            imgView.setImageDrawable(result.getDrawable());
        };
        return handlers;
    }

    public static Handlers<ImageView> centerInside(Handlers<ImageView> handlers) {
        handlers.success = (viewHolder, result) -> {
            ImageView imgView = viewHolder.get();
            imgView.setImageDrawable(null);
            imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imgView.setColorFilter(null);
            imgView.setImageDrawable(result.getDrawable());
        };
        return handlers;
    }

    public static Handlers<ImageView> withPlaceholder(Handlers<ImageView> handlers, int drawableRes) {
        handlers.placeholder = viewHolder -> {
            centerRes(viewHolder, drawableRes);
            viewHolder.get().setColorFilter(null);
        };
        return handlers;
    }

    public static Handlers<ImageView> withPlaceholder(Handlers<ImageView> handlers, int drawableRes, int tintColor) {
        handlers.placeholder = viewHolder -> {
            centerRes(viewHolder, drawableRes);
            tint(viewHolder, tintColor);
        };
        return handlers;
    }

    public static Handlers<ImageView> whenError(Handlers<ImageView> handlers, int drawableRes, int tintColor) {
        handlers.error = viewHolder -> {
            centerRes(viewHolder, drawableRes);
            tint(viewHolder, tintColor);
        };
        return handlers;
    }

    public static void centerRes(ViewHolder<ImageView> viewHolder, int drawableRes) {
        ImageView imgView = viewHolder.get();
        imgView.setScaleType(ImageView.ScaleType.CENTER);
        imgView.setImageResource(drawableRes);
    }

    public static void tint(ViewHolder<ImageView> viewHolder, int tintColor) {
        ImageView imgView = viewHolder.get();
        imgView.setColorFilter(tintColor, PorterDuff.Mode.MULTIPLY);
    }
}
