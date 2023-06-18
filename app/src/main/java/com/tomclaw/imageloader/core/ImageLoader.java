package com.tomclaw.imageloader.core;

import java.security.NoSuchAlgorithmException;

public interface ImageLoader {

    <T> void load(ViewHolder<T> view, String uriString, Handlers<T> handlers) throws NoSuchAlgorithmException;

}
