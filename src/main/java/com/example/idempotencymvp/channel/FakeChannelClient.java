package com.example.idempotencymvp.channel;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class FakeChannelClient {

    private final AtomicInteger invocationCount = new AtomicInteger();
    private final List<String> receivedIdempotencyKeys =
            Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<String, ChannelPaymentResult> results =
            new ConcurrentHashMap<>();

    public ChannelPaymentResult createPayment(
            String businessIdempotencyKey, BigDecimal amount, String currency) {
        invocationCount.incrementAndGet();
        receivedIdempotencyKeys.add(businessIdempotencyKey);
        return results.computeIfAbsent(
                businessIdempotencyKey,
                key -> new ChannelPaymentResult("fake_" + UUID.nameUUIDFromBytes(
                        key.getBytes(StandardCharsets.UTF_8))));
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public List<String> getReceivedIdempotencyKeys() {
        synchronized (receivedIdempotencyKeys) {
            return new ArrayList<>(receivedIdempotencyKeys);
        }
    }

    public void reset() {
        invocationCount.set(0);
        receivedIdempotencyKeys.clear();
        results.clear();
    }
}
