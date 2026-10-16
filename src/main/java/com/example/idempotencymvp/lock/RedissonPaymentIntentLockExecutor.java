package com.example.idempotencymvp.lock;

import com.example.idempotencymvp.exception.CallbackBusyException;
import com.example.idempotencymvp.idempotency.IdempotencyKeyFactory;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.redisson.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonPaymentIntentLockExecutor implements PaymentIntentLockExecutor {

    private final RedissonClient redissonClient;

    public RedissonPaymentIntentLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String paymentIntentId, LockedOperation<T> operation) {
        RLock lock = redissonClient.getLock(
                IdempotencyKeyFactory.PREFIX + "payment_intent_processing_" + paymentIntentId);
        boolean locked = false;
        try {
            // 不等待锁：同一支付的并发回调立即失败，避免请求线程排队和重复执行业务。
            locked = lock.tryLock(0, 20, TimeUnit.SECONDS);
            if (!locked) {
                throw new CallbackBusyException(paymentIntentId);
            }
            return operation.execute();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring callback lock", interrupted);
        } finally {
            // leaseTime 到期后锁可能已自动释放，必须确认当前线程仍持有锁。
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
