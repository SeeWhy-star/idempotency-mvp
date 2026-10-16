package com.example.idempotencymvp.lock;

public interface PaymentIntentLockExecutor {

    <T> T execute(String paymentIntentId, LockedOperation<T> operation);
}
