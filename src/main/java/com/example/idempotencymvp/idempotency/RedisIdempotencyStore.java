package com.example.idempotencymvp.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.idempotency.store",
        havingValue = "redis",
        matchIfMissing = true)
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final DefaultRedisScript<List> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local status = redis.call('HGET', KEYS[1], 'status'); "
                    + "if not status then "
                    + "redis.call('HSET', KEYS[1], "
                    + "'status', 'PROCESSING', 'fingerprint', ARGV[1], "
                    + "'ownerToken', ARGV[2], 'createdAt', ARGV[3]); "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[4]); "
                    + "return {'ACQUIRED', ARGV[1], ARGV[2], '', '', ARGV[3]}; end; "
                    + "local fingerprint = redis.call('HGET', KEYS[1], 'fingerprint'); "
                    + "if fingerprint ~= ARGV[1] then return {'PARAMETER_MISMATCH'}; end; "
                    + "if status == 'COMPLETED' then return {'COMPLETED', fingerprint, "
                    + "redis.call('HGET', KEYS[1], 'ownerToken') or '', "
                    + "redis.call('HGET', KEYS[1], 'httpStatus') or '', "
                    + "redis.call('HGET', KEYS[1], 'responseBody') or '', "
                    + "redis.call('HGET', KEYS[1], 'createdAt') or ''}; end; "
                    + "return {'PROCESSING'};",
            List.class);

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], 'status') == 'PROCESSING' "
                    + "and redis.call('HGET', KEYS[1], 'ownerToken') == ARGV[1] "
                    + "and redis.call('HGET', KEYS[1], 'fingerprint') == ARGV[2] then "
                    + "redis.call('HSET', KEYS[1], 'status', 'COMPLETED', "
                    + "'httpStatus', ARGV[3], 'responseBody', ARGV[4]); "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[5]); return 1; end; return 0;",
            Long.class);

    private static final DefaultRedisScript<Long> ABORT_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], 'status') == 'PROCESSING' "
                    + "and redis.call('HGET', KEYS[1], 'ownerToken') == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]); end; return 0;",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration processingTtl;
    private final Duration completedTtl;

    public RedisIdempotencyStore(
            StringRedisTemplate redisTemplate,
            @Value("${app.idempotency.processing-ttl:30s}") Duration processingTtl,
            @Value("${app.idempotency.completed-ttl:24h}") Duration completedTtl) {
        this.redisTemplate = redisTemplate;
        this.processingTtl = processingTtl;
        this.completedTtl = completedTtl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AcquireResult acquire(String key, String fingerprint, String ownerToken) {
        Instant createdAt = Instant.now();
        List<Object> values = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                Collections.singletonList(key),
                fingerprint,
                ownerToken,
                createdAt.toString(),
                Long.toString(processingTtl.toMillis()));
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Redis acquire script returned no result");
        }

        AcquireDecision decision = AcquireDecision.valueOf(asString(values.get(0)));
        if (decision == AcquireDecision.ACQUIRED) {
            return AcquireResult.of(
                    decision,
                    IdempotencyRecord.processing(fingerprint, ownerToken, createdAt));
        }
        if (decision == AcquireDecision.COMPLETED) {
            IdempotencyRecord record = new IdempotencyRecord(
                    IdempotencyStatus.COMPLETED,
                    asString(values.get(1)),
                    asString(values.get(2)),
                    Integer.valueOf(asString(values.get(3))),
                    asString(values.get(4)),
                    Instant.parse(asString(values.get(5))));
            return AcquireResult.of(decision, record);
        }
        return AcquireResult.of(decision, null);
    }

    @Override
    public boolean complete(
            String key,
            String fingerprint,
            String ownerToken,
            int httpStatus,
            String responseBody) {
        Long changed = redisTemplate.execute(
                COMPLETE_SCRIPT,
                Collections.singletonList(key),
                ownerToken,
                fingerprint,
                Integer.toString(httpStatus),
                responseBody,
                Long.toString(completedTtl.toMillis()));
        return Long.valueOf(1L).equals(changed);
    }

    @Override
    public boolean abort(String key, String ownerToken) {
        Long changed = redisTemplate.execute(
                ABORT_SCRIPT, Collections.singletonList(key), ownerToken);
        return Long.valueOf(1L).equals(changed);
    }

    private String asString(Object value) {
        if (value instanceof byte[]) {
            return redisTemplate.getStringSerializer().deserialize((byte[]) value);
        }
        return String.valueOf(value);
    }
}
