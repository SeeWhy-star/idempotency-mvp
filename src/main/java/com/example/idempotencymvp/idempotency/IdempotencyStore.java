package com.example.idempotencymvp.idempotency;

public interface IdempotencyStore {

    AcquireResult acquire(String key, String fingerprint, String ownerToken);

    boolean complete(
            String key,
            String fingerprint,
            String ownerToken,
            int httpStatus,
            String responseBody);

    boolean abort(String key, String ownerToken);
}
