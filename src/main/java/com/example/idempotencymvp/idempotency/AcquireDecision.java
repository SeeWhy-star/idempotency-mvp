package com.example.idempotencymvp.idempotency;

public enum AcquireDecision {
    ACQUIRED,
    PROCESSING,
    COMPLETED,
    PARAMETER_MISMATCH
}
