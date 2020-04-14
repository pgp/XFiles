package it.pgp.xfiles.utils;

import android.content.Context;
import android.widget.Toast;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CheckableSingleExecutor implements ExecutorService {

    private final Context context;
    private final ThreadPoolExecutor singleThreadExecutor;

    public CheckableSingleExecutor(Context context) {
        this.context = context;
        singleThreadExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

    public boolean isBusy() {
        if(singleThreadExecutor.getActiveCount() > 0) {
            Toast.makeText(context, "Current goDir thread is busy", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void shutdown() {
        singleThreadExecutor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return singleThreadExecutor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return singleThreadExecutor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return singleThreadExecutor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return singleThreadExecutor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return isBusy()?null:singleThreadExecutor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return isBusy()?null:singleThreadExecutor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return isBusy()?null:singleThreadExecutor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return isBusy()?null:singleThreadExecutor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return isBusy()?null:singleThreadExecutor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return isBusy()?null:singleThreadExecutor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return isBusy()?null:singleThreadExecutor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if(isBusy()) throw new RuntimeException("Single thread is busy");
        singleThreadExecutor.execute(command);
    }
}
