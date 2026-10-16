package com.example.idempotencymvp.service;

import com.example.idempotencymvp.channel.ChannelPaymentResult;
import com.example.idempotencymvp.controller.CreatePaymentIntentRequest;
import com.example.idempotencymvp.controller.CreatePaymentIntentResponse;
import com.example.idempotencymvp.domain.PaymentIntent;
import com.example.idempotencymvp.domain.PaymentIntentStatus;
import com.example.idempotencymvp.exception.ResourceNotFoundException;
import com.example.idempotencymvp.repository.PaymentIntentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentIntentService {

    private final PaymentIntentRepository repository;
    private final ChannelSubmissionService channelSubmissionService;

    public PaymentIntentService(
            PaymentIntentRepository repository,
            ChannelSubmissionService channelSubmissionService) {
        this.repository = repository;
        this.channelSubmissionService = channelSubmissionService;
    }

    @Transactional
    public CreatePaymentIntentResponse create(
            String accountId, CreatePaymentIntentRequest request) {
        return repository.findByAccountIdAndMerchantOrderNo(
                        accountId, request.getMerchantOrderNo())
                .map(this::toResponse)
                .orElseGet(() -> createNew(accountId, request));
    }

    @Transactional
    public ChannelPaymentResult retryChannelSubmission(String paymentIntentId) {
        PaymentIntent paymentIntent = repository.findById(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment intent not found: " + paymentIntentId));
        return channelSubmissionService.submit(paymentIntent);
    }

    private CreatePaymentIntentResponse createNew(
            String accountId, CreatePaymentIntentRequest request) {
        Instant now = Instant.now();
        PaymentIntent paymentIntent = new PaymentIntent(
                UUID.randomUUID().toString(),
                accountId,
                request.getMerchantOrderNo(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                PaymentIntentStatus.PROCESSING,
                now);
        repository.saveAndFlush(paymentIntent);

        ChannelPaymentResult channelResult = channelSubmissionService.submit(paymentIntent);
        paymentIntent.setChannelTransactionId(channelResult.getChannelTransactionId());
        repository.save(paymentIntent);
        return toResponse(paymentIntent);
    }

    private CreatePaymentIntentResponse toResponse(PaymentIntent paymentIntent) {
        return new CreatePaymentIntentResponse(
                paymentIntent.getId(),
                paymentIntent.getMerchantOrderNo(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency(),
                paymentIntent.getStatus());
    }
}
