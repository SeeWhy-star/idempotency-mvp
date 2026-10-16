package com.example.idempotencymvp.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.idempotencymvp.exception.IdempotencyConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotencyServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private InMemoryIdempotencyStore store;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore(
                Duration.ofSeconds(30), Duration.ofHours(24), clock);
        service = new IdempotencyService(store, new ObjectMapper());
    }

    @AfterEach
    void shutDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    void firstRequestExecutesBusinessAndReturnsSuccess() throws Throwable {
        AtomicInteger calls = new AtomicInteger();

        ResponseEntity<?> response = service.execute(
                context("owner-1", "fingerprint-1"),
                () -> {
                    calls.incrementAndGet();
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(Collections.singletonMap("id", "pi_1"));
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(calls).hasValue(1);
    }

    @Test
    void sameKeyAndParametersReplayFirstJsonWithoutExecutingAgain() throws Throwable {
        AtomicInteger calls = new AtomicInteger();
        IdempotentOperation operation = () -> {
            calls.incrementAndGet();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.singletonMap("id", "pi_1"));
        };

        ResponseEntity<?> first = service.execute(
                context("owner-1", "fingerprint-1"), operation);
        ResponseEntity<?> replay = service.execute(
                context("owner-2", "fingerprint-1"), operation);

        assertThat(calls).hasValue(1);
        assertThat(replay.getStatusCode()).isEqualTo(first.getStatusCode());
        assertThat(replay.getBody().toString()).isEqualTo("{\"id\":\"pi_1\"}");
    }

    @Test
    void sameKeyWithDifferentParametersReturnsConflict() throws Throwable {
        service.execute(
                context("owner-1", "fingerprint-1"),
                () -> ResponseEntity.ok(Collections.singletonMap("id", "pi_1")));

        assertThatThrownBy(() -> service.execute(
                context("owner-2", "fingerprint-2"),
                () -> ResponseEntity.ok("must-not-run")))
                .isInstanceOf(IdempotencyConflictException.class)
                .extracting("code")
                .isEqualTo("parameter_mismatch");
    }

    @Test
    void twoConcurrentRequestsAllowOnlyOneIntoBusiness() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger businessCalls = new AtomicInteger();

        Future<ResponseEntity<?>> first = executor.submit(() -> {
            try {
                return service.execute(context("owner-1", "same-fingerprint"), () -> {
                    businessCalls.incrementAndGet();
                    firstEntered.countDown();
                    if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test release timed out");
                    }
                    return ResponseEntity.ok(Collections.singletonMap("id", "pi_1"));
                });
            } catch (Throwable failure) {
                throw new RuntimeException(failure);
            }
        });

        assertThat(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();
        Future<String> second = executor.submit(() -> {
            try {
                service.execute(
                        context("owner-2", "same-fingerprint"),
                        () -> {
                            businessCalls.incrementAndGet();
                            return ResponseEntity.ok("must-not-run");
                        });
                return "unexpected-success";
            } catch (IdempotencyConflictException conflict) {
                return conflict.getCode();
            } catch (Throwable failure) {
                throw new RuntimeException(failure);
            }
        });

        assertThat(second.get(2, TimeUnit.SECONDS)).isEqualTo("request_processing");
        releaseFirst.countDown();
        assertThat(first.get(2, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(businessCalls).hasValue(1);
    }

    @Test
    void processingRequestReturnsExplicitConflictWithoutPolling() {
        store.acquire("test-key", "fingerprint-1", "owner-1");

        assertThatThrownBy(() -> service.execute(
                context("owner-2", "fingerprint-1"),
                () -> ResponseEntity.ok("must-not-run")))
                .isInstanceOf(IdempotencyConflictException.class)
                .extracting("code")
                .isEqualTo("request_processing");
    }

    @Test
    void businessFailureAbortsOwnedProcessingAndNextAttemptCanRetry() throws Throwable {
        assertThatThrownBy(() -> service.execute(
                context("owner-1", "fingerprint-1"),
                () -> {
                    throw new IllegalStateException("simulated business failure");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated business failure");

        ResponseEntity<?> retried = service.execute(
                context("owner-2", "fingerprint-1"),
                () -> ResponseEntity.ok(Collections.singletonMap("retried", true)));

        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nonOwnerCannotCompleteOrAbortAnotherOwnersRecord() {
        store.acquire("test-key", "fingerprint-1", "owner-1");

        assertThat(store.complete(
                "test-key", "fingerprint-1", "owner-2", 200, "{}"))
                .isFalse();
        assertThat(store.abort("test-key", "owner-2")).isFalse();
        assertThat(store.acquire("test-key", "fingerprint-1", "owner-3").getDecision())
                .isEqualTo(AcquireDecision.PROCESSING);
    }

    @Test
    void expiredProcessingRecordCanBeAcquiredAgain() {
        store.acquire("test-key", "fingerprint-1", "owner-1");
        clock.advance(Duration.ofSeconds(31));

        AcquireResult retried = store.acquire("test-key", "fingerprint-1", "owner-2");

        assertThat(retried.getDecision()).isEqualTo(AcquireDecision.ACQUIRED);
        assertThat(retried.getRecord().getOwnerToken()).isEqualTo("owner-2");
    }

    private IdempotencyContext context(String ownerToken, String fingerprint) {
        return new IdempotencyContext("test-key", fingerprint, ownerToken);
    }
}
