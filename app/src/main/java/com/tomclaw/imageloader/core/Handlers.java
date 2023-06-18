package com.tomclaw.imageloader.core;

public class Handlers<T> {
    @FunctionalInterface
    public interface SuccessInterface<T> {
        void fn(ViewHolder<T> viewHolder, Result result);
    }

    @FunctionalInterface
    public interface PlaceholderOrErrorInterface<T> {
        void fn(ViewHolder<T> viewHolder);
    }

    public SuccessInterface<T> success = (v,r) -> {};
    public PlaceholderOrErrorInterface<T> placeholder = v -> {};
    public PlaceholderOrErrorInterface<T> error = v -> {};

    public void setPlaceholder(PlaceholderOrErrorInterface<T> placeholder) {
        this.placeholder = placeholder;
    }

    public void setError(PlaceholderOrErrorInterface<T> error) {
        this.error = error;
    }

    public void setSuccess(SuccessInterface<T> success) {
        this.success = success;
    }
}
