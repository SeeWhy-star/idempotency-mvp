package com.example.idempotencymvp.domain;

public enum PaymentIntentStatus {
    PROCESSING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED
}
