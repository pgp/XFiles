package com.tomclaw.imageloader.core;

import android.support.annotation.Nullable;

import java.io.File;

public interface Decoder {

    boolean probe(File file);

    @Nullable Result decode(File file, int width, int height);
}
