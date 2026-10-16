package com.example.idempotencymvp.exception;

public class CallbackBusyException extends RuntimeException {

    public CallbackBusyException(String paymentIntentId) {
        super("Payment callback is already being processed: " + paymentIntentId);
    }
}
