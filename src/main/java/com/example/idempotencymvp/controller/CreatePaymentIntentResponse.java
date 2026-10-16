package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.domain.PaymentIntentStatus;
import java.math.BigDecimal;

public class CreatePaymentIntentResponse {

    private final String id;
    private final String merchantOrderNo;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentIntentStatus status;

    public CreatePaymentIntentResponse(
            String id,
            String merchantOrderNo,
            BigDecimal amount,
            String currency,
            PaymentIntentStatus status) {
        this.id = id;
        this.merchantOrderNo = merchantOrderNo;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }
}
