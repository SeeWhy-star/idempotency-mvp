package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.domain.PaymentIntentStatus;

public record PaymentCallbackResponse(
        boolean accepted,
        boolean transitioned,
        String paymentIntentId,
        PaymentIntentStatus status) {
}
