package com.example.idempotencymvp.controller;

import com.example.idempotencymvp.annotation.Idempotent;
import com.example.idempotencymvp.service.PaymentCallbackService;
import com.example.idempotencymvp.service.PaymentIntentService;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1")
public class PaymentIntentController {

    private final PaymentIntentService paymentIntentService;
    private final PaymentCallbackService paymentCallbackService;

    public PaymentIntentController(
            PaymentIntentService paymentIntentService,
            PaymentCallbackService paymentCallbackService) {
        this.paymentIntentService = paymentIntentService;
        this.paymentCallbackService = paymentCallbackService;
    }

    @Idempotent
    @PostMapping("/payment_intents")
    public ResponseEntity<CreatePaymentIntentResponse> create(
            @RequestHeader("X-Account-Id") @Size(max = 100) String accountId,
            @RequestHeader("Idempotency-Key") @Size(max = 255) String idempotencyKey,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentIntentService.create(accountId, request));
    }

    @PostMapping("/callbacks/payments/{paymentIntentId}")
    public ResponseEntity<PaymentCallbackResponse> callback(
            @PathVariable String paymentIntentId,
            @Valid @RequestBody PaymentCallbackRequest request) {
        return ResponseEntity.ok(paymentCallbackService.handle(paymentIntentId, request));
    }
}
