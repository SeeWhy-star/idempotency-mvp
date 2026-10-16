package com.example.idempotencymvp.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.idempotency.store", havingValue = "memory")
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, Entry> records = new ConcurrentHashMap<>();
    private final Duration processingTtl;
    private final Duration completedTtl;
    private final Clock clock;

    @Autowired
    public InMemoryIdempotencyStore(
            @Value("${app.idempotency.processing-ttl:30s}") Duration processingTtl,
            @Value("${app.idempotency.completed-ttl:24h}") Duration completedTtl) {
        this(processingTtl, completedTtl, Clock.systemUTC());
    }

    public InMemoryIdempotencyStore(
            Duration processingTtl, Duration completedTtl, Clock clock) {
        this.processingTtl = processingTtl;
        this.completedTtl = completedTtl;
        this.clock = clock;
    }

    @Override
    public AcquireResult acquire(String key, String fingerprint, String ownerToken) {
        AtomicReference<AcquireResult> result = new AtomicReference<>();
        Instant now = clock.instant();

        records.compute(key, (ignored, current) -> {
            if (current == null || !current.expiresAt.isAfter(now)) {
                IdempotencyRecord record =
                        IdempotencyRecord.processing(fingerprint, ownerToken, now);
                result.set(AcquireResult.of(AcquireDecision.ACQUIRED, record));
                return new Entry(record, now.plus(processingTtl));
            }
            if (!current.record.getFingerprint().equals(fingerprint)) {
                result.set(AcquireResult.of(
                        AcquireDecision.PARAMETER_MISMATCH, current.record));
                return current;
            }
            AcquireDecision decision = current.record.getStatus() == IdempotencyStatus.COMPLETED
                    ? AcquireDecision.COMPLETED
                    : AcquireDecision.PROCESSING;
            result.set(AcquireResult.of(decision, current.record));
            return current;
        });
        return result.get();
    }

    @Override
    public boolean complete(
            String key,
            String fingerprint,
            String ownerToken,
            int httpStatus,
            String responseBody) {
        AtomicReference<Boolean> completed = new AtomicReference<>(false);
        Instant now = clock.instant();
        records.computeIfPresent(key, (ignored, current) -> {
            IdempotencyRecord record = current.record;
            if (record.getStatus() == IdempotencyStatus.PROCESSING
                    && record.getOwnerToken().equals(ownerToken)
                    && record.getFingerprint().equals(fingerprint)
                    && current.expiresAt.isAfter(now)) {
                completed.set(true);
                return new Entry(
                        record.completed(httpStatus, responseBody), now.plus(completedTtl));
            }
            return current.expiresAt.isAfter(now) ? current : null;
        });
        return completed.get();
    }

    @Override
    public boolean abort(String key, String ownerToken) {
        AtomicReference<Boolean> aborted = new AtomicReference<>(false);
        records.computeIfPresent(key, (ignored, current) -> {
            if (current.record.getStatus() == IdempotencyStatus.PROCESSING
                    && current.record.getOwnerToken().equals(ownerToken)) {
                aborted.set(true);
                return null;
            }
            return current;
        });
        return aborted.get();
    }

    private static final class Entry {
        private final IdempotencyRecord record;
        private final Instant expiresAt;

        private Entry(IdempotencyRecord record, Instant expiresAt) {
            this.record = record;
            this.expiresAt = expiresAt;
        }
    }
}
