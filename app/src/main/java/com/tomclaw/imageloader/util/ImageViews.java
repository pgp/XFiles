package com.tomclaw.imageloader.util;

import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.tomclaw.imageloader.SimpleImageLoader;
import com.tomclaw.imageloader.core.Handlers;

public class ImageViews {

    // translated from extension function for Handlers<ImageView>
    @FunctionalInterface
    public interface handlersFn {
        void fn(Handlers<ImageView> handlers);
    }

    static final handlersFn emptyHandlersFn = h -> {};

    public static void fetch(ImageView imageView, String url, @Nullable handlersFn params) {
        Handlers<ImageView> handlers = new Handlers<>();
        handlers.success = (viewHolder, result) -> viewHolder.get().setImageDrawable(result.getDrawable());
        if(params == null) params = emptyHandlersFn;
        params.fn(handlers);
        ImageViewHolder viewHolder = new ImageViewHolder(imageView);
        try {
            SimpleImageLoader.getImageLoader(imageView.getContext()).load(viewHolder, url, handlers);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
