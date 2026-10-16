package com.example.idempotencymvp.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Tag("redis")
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TESTS", matches = "true")
class RedisIdempotencyStoreIntegrationTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate template;
    private RedisIdempotencyStore store;
    private String key;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6380);
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        store = new RedisIdempotencyStore(
                template, Duration.ofSeconds(30), Duration.ofHours(24));
        key = IdempotencyKeyFactory.PREFIX + "test:" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        template.delete(key);
        connectionFactory.destroy();
    }

    @Test
    void luaScriptsEnforceOwnerAndReplayCompletedResponse() {
        assertThat(store.acquire(key, "fp-1", "owner-1").getDecision())
                .isEqualTo(AcquireDecision.ACQUIRED);
        assertThat(store.complete(key, "fp-1", "owner-2", 201, "{\"id\":\"pi_1\"}"))
                .isFalse();
        assertThat(store.abort(key, "owner-2")).isFalse();
        assertThat(store.complete(key, "fp-1", "owner-1", 201, "{\"id\":\"pi_1\"}"))
                .isTrue();

        AcquireResult replay = store.acquire(key, "fp-1", "owner-3");
        assertThat(replay.getDecision()).isEqualTo(AcquireDecision.COMPLETED);
        assertThat(replay.getRecord().getHttpStatus()).isEqualTo(201);
        assertThat(replay.getRecord().getResponseBody()).isEqualTo("{\"id\":\"pi_1\"}");
    }
}
