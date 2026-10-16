package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.domain.PaymentIntentStatus;

public class PaymentCallbackResponse {

    private final boolean accepted;
    private final boolean transitioned;
    private final String paymentIntentId;
    private final PaymentIntentStatus status;

    public PaymentCallbackResponse(
            boolean accepted,
            boolean transitioned,
            String paymentIntentId,
            PaymentIntentStatus status) {
        this.accepted = accepted;
        this.transitioned = transitioned;
        this.paymentIntentId = paymentIntentId;
        this.status = status;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isTransitioned() {
        return transitioned;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }
}
