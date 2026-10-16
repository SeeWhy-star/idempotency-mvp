package com.example.idempotencymvp.idempotency;

import java.time.Instant;

public class IdempotencyRecord {

    private final IdempotencyStatus status;
    private final String fingerprint;
    private final String ownerToken;
    private final Integer httpStatus;
    private final String responseBody;
    private final Instant createdAt;

    public IdempotencyRecord(
            IdempotencyStatus status,
            String fingerprint,
            String ownerToken,
            Integer httpStatus,
            String responseBody,
            Instant createdAt) {
        this.status = status;
        this.fingerprint = fingerprint;
        this.ownerToken = ownerToken;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecord processing(
            String fingerprint, String ownerToken, Instant createdAt) {
        return new IdempotencyRecord(
                IdempotencyStatus.PROCESSING,
                fingerprint,
                ownerToken,
                null,
                null,
                createdAt);
    }

    public IdempotencyRecord completed(int statusCode, String body) {
        return new IdempotencyRecord(
                IdempotencyStatus.COMPLETED,
                fingerprint,
                ownerToken,
                statusCode,
                body,
                createdAt);
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
