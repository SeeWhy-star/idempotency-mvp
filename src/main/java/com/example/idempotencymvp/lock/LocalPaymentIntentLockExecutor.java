package com.example.idempotencymvp.lock;

import com.example.idempotencymvp.exception.CallbackBusyException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redisson.enabled", havingValue = "false")
public class LocalPaymentIntentLockExecutor implements PaymentIntentLockExecutor {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T execute(String paymentIntentId, LockedOperation<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(
                paymentIntentId, ignored -> new ReentrantLock());
        boolean locked = lock.tryLock();
        if (!locked) {
            throw new CallbackBusyException(paymentIntentId);
        }
        try {
            return operation.execute();
        } finally {
            lock.unlock();
            locks.remove(paymentIntentId, lock);
        }
    }
}
