package com.tomclaw.imageloader.core;

import android.graphics.drawable.Drawable;

public interface Result {

    int getByteCount();

    boolean isRecycled();

    Drawable getDrawable();
}
