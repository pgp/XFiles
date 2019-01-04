package it.pgp.xfiles.service;

public interface ResultInterface<T> {
    T method(Object... params);
}