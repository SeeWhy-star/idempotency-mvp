package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.domain.PaymentIntentStatus;
import java.math.BigDecimal;

public record CreatePaymentIntentResponse(
        String id,
        String merchantOrderNo,
        BigDecimal amount,
        String currency,
        PaymentIntentStatus status) {
}
