package com.example.idempotencymvp.service;

import com.example.idempotencymvp.controller.PaymentCallbackRequest;
import com.example.idempotencymvp.controller.PaymentCallbackResponse;
import com.example.idempotencymvp.lock.PaymentIntentLockExecutor;
import org.springframework.stereotype.Service;

@Service
public class PaymentCallbackService {

    private final PaymentIntentLockExecutor lockExecutor;
    private final PaymentIntentTransitionService transitionService;

    public PaymentCallbackService(
            PaymentIntentLockExecutor lockExecutor,
            PaymentIntentTransitionService transitionService) {
        this.lockExecutor = lockExecutor;
        this.transitionService = transitionService;
    }

    public PaymentCallbackResponse handle(
            String paymentIntentId, PaymentCallbackRequest request) {
        return lockExecutor.execute(
                paymentIntentId,
                () -> transitionService.transition(paymentIntentId, request));
    }
}
