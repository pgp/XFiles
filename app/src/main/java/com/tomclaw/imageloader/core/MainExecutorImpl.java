package com.tomclaw.imageloader.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class MainExecutorImpl implements Executor {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isMainThread() {
        return mainHandler.getLooper().getThread() == Thread.currentThread();
    }

    @Override
    public void execute(Runnable runnable) {
        if(isMainThread()) runnable.run();
        else mainHandler.post(runnable);
    }
}
