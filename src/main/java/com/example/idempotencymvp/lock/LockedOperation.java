package com.example.idempotencymvp.lock;

@FunctionalInterface
public interface LockedOperation<T> {

    T execute();
}
