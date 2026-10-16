package com.example.idempotencymvp.idempotency;

public class IdempotencyContext {

    private final String redisKey;
    private final String fingerprint;
    private final String ownerToken;

    public IdempotencyContext(String redisKey, String fingerprint, String ownerToken) {
        this.redisKey = redisKey;
        this.fingerprint = fingerprint;
        this.ownerToken = ownerToken;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getOwnerToken() {
        return ownerToken;
    }
}
