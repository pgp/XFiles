package com.tomclaw.imageloader.core;

import android.support.annotation.Nullable;

public interface ViewHolder<T> {

    @Nullable ViewSize optSize();

    ViewSize getSize();

    @Nullable Object getTag();

    void setTag(Object tag);

    T get();
}