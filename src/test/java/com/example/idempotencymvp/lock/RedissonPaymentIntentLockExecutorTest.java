package com.example.idempotencymvp.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.idempotencymvp.exception.CallbackBusyException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class RedissonPaymentIntentLockExecutorTest {

    @Test
    void lockFailureNeverExecutesBusiness() throws InterruptedException {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redisson.getLock("idempotency:mvp:payment_intent_processing_pi_1"))
                .thenReturn(lock);
        when(lock.tryLock(0, 20, TimeUnit.SECONDS)).thenReturn(false);
        AtomicInteger businessCalls = new AtomicInteger();
        RedissonPaymentIntentLockExecutor executor =
                new RedissonPaymentIntentLockExecutor(redisson);

        assertThatThrownBy(() -> executor.execute("pi_1", () -> {
            businessCalls.incrementAndGet();
            return "unexpected";
        })).isInstanceOf(CallbackBusyException.class);

        assertThat(businessCalls).hasValue(0);
        verify(lock, never()).unlock();
    }

    @Test
    void ownedLockIsReleasedAfterBusinessCompletes() throws InterruptedException {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redisson.getLock("idempotency:mvp:payment_intent_processing_pi_1"))
                .thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RedissonPaymentIntentLockExecutor executor =
                new RedissonPaymentIntentLockExecutor(redisson);

        assertThat(executor.execute("pi_1", () -> "done")).isEqualTo("done");
        verify(lock).unlock();
    }
}
