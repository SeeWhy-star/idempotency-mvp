package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.domain.PaymentIntentStatus;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PaymentCallbackRequest {

    @NotBlank
    private String channelTransactionId;

    @NotNull
    private PaymentIntentStatus status;

    public String getChannelTransactionId() {
        return channelTransactionId;
    }

    public void setChannelTransactionId(String channelTransactionId) {
        this.channelTransactionId = channelTransactionId;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentIntentStatus status) {
        this.status = status;
    }
}
