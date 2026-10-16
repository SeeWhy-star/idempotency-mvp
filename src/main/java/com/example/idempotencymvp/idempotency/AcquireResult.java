package com.example.idempotencymvp.idempotency;

public class AcquireResult {

    private final AcquireDecision decision;
    private final IdempotencyRecord record;

    private AcquireResult(AcquireDecision decision, IdempotencyRecord record) {
        this.decision = decision;
        this.record = record;
    }

    public static AcquireResult of(AcquireDecision decision, IdempotencyRecord record) {
        return new AcquireResult(decision, record);
    }

    public AcquireDecision getDecision() {
        return decision;
    }

    public IdempotencyRecord getRecord() {
        return record;
    }
}
